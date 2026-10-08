use crate::config::Config;
use axum::body::Body;
use axum::extract::State;
use axum::http::{HeaderMap, Request, StatusCode};
use axum::response::Response;
use axum::routing::{any, get};
use axum::{Json, Router};
use futures_util::StreamExt;
use serde::Deserialize;
use serde::Serialize;
use std::net::SocketAddr;
use std::sync::Arc;

mod config;

#[derive(Clone)]
struct AppState {
    config: Arc<Config>,
}

#[derive(Deserialize, Serialize)]
struct RateLimitCheckRequest {
    #[serde(rename = "clientIp")]
    client_ip: String,
    service: String,
}

#[derive(Deserialize)]
struct RateLimitCheckResponse {
    allowed: bool,
}

#[derive(Deserialize)]
struct InstanceResponse {
    host: String,
    port: u16,
}

#[derive(Serialize)]
struct HealthResponse {
    status: &'static str,
}

async fn health() -> Json<HealthResponse> {
    Json(HealthResponse { status: "UP" })
}

async fn proxy(State(state): State<AppState>, req: Request<Body>) -> Response {
    let path = req.uri().path().to_string();

    let route = match state.config.route_for(&path) {
        Some(r) => r,
        None => return not_found(),
    };

    let client_ip = extract_client_ip(&req);

    if !rate_limit_allows(&state, &client_ip, &route.service).await {
        return too_many_requests();
    }

    let instance = match load_balance(&state, &route.service).await {
        Some(i) => i,
        None => return service_unavailable(),
    };

    forward(&state, req, &instance).await
}

async fn rate_limit_allows(state: &AppState, client_ip: &str, service: &str) -> bool {
    let client = reqwest::Client::new();
    let body = RateLimitCheckRequest {
        client_ip: client_ip.to_string(),
        service: service.to_string(),
    };

    let url = format!("{}/api/v1/check", state.config.rate_limiter.url);
    match client.post(&url).json(&body).send().await {
        Ok(resp) => match resp.json::<RateLimitCheckResponse>().await {
            Ok(check) => check.allowed,
            Err(_) => true,
        },
        Err(_) => true,
    }
}

async fn load_balance(state: &AppState, service: &str) -> Option<InstanceResponse> {
    let client = reqwest::Client::new();
    let url = format!(
        "{}/api/v1/instances/{}",
        state.config.load_balancer.url, service
    );

    match client.get(&url).send().await {
        Ok(resp) => resp.json::<InstanceResponse>().await.ok(),
        Err(_) => None,
    }
}

async fn forward(state: &AppState, req: Request<Body>, instance: &InstanceResponse) -> Response {
    let (parts, body) = req.into_parts();

    let target = format!("http://{}:{}{}", instance.host, instance.port, parts.uri);
    let method = parts.method.clone();

    let headers = parts.headers.clone();
    let _ = state; // config not needed for forward

    let client = reqwest::Client::new();
    let mut request = client
        .request(method, &target)
        .headers(filter_hop_by_hop(headers));

    if let Some(stream) = body_into_stream(body) {
        request = request.body(reqwest::Body::wrap_stream(stream));
    } else {
        request = request.body(reqwest::Body::default());
    }

    match request.send().await {
        Ok(resp) => {
            let status = resp.status();
            let resp_headers = resp.headers().clone();
            let bytes = resp.bytes().await.unwrap_or_default();

            let mut builder = Response::builder().status(status);
            for (name, value) in resp_headers.iter() {
                if !is_hop_by_hop(name.as_str()) {
                    builder = builder.header(name, value);
                }
            }
            builder
                .body(Body::from(bytes))
                .unwrap_or_else(|_| Response::builder().status(500).body(Body::empty()).unwrap())
        }
        Err(e) => {
            tracing::error!("Upstream request failed: {}", e);
            bad_gateway()
        }
    }
}

fn body_into_stream(body: Body) -> Option<impl futures_util::Stream<Item = Result<bytes::Bytes, std::io::Error>>> {
    Some(
        body.into_data_stream()
            .map(|result| result.map_err(|e| std::io::Error::other(e.to_string()))),
    )
}

fn extract_client_ip(req: &Request<Body>) -> String {
    req.headers()
        .get("x-forwarded-for")
        .and_then(|v| v.to_str().ok())
        .and_then(|v| v.split(',').next())
        .map(|s| s.trim().to_string())
        .unwrap_or_else(|| "unknown".to_string())
}

fn filter_hop_by_hop(headers: HeaderMap) -> HeaderMap {
    let mut filtered = HeaderMap::new();
    for (name, value) in headers.iter() {
        if !is_hop_by_hop(name.as_str()) {
            let _ = filtered.append(name, value.clone());
        }
    }
    filtered
}

fn is_hop_by_hop(name: &str) -> bool {
    matches!(
        name.to_ascii_lowercase().as_str(),
        "connection"
            | "keep-alive"
            | "proxy-authenticate"
            | "proxy-authorization"
            | "te"
            | "trailer"
            | "transfer-encoding"
            | "upgrade"
            | "host"
    )
}

fn not_found() -> Response {
    Response::builder()
        .status(StatusCode::NOT_FOUND)
        .body(Body::from("{\"error\":\"Not Found\"}"))
        .unwrap()
}

fn too_many_requests() -> Response {
    Response::builder()
        .status(StatusCode::TOO_MANY_REQUESTS)
        .body(Body::from("{\"error\":\"Too Many Requests\"}"))
        .unwrap()
}

fn service_unavailable() -> Response {
    Response::builder()
        .status(StatusCode::SERVICE_UNAVAILABLE)
        .body(Body::from("{\"error\":\"Service Unavailable\"}"))
        .unwrap()
}

fn bad_gateway() -> Response {
    Response::builder()
        .status(StatusCode::BAD_GATEWAY)
        .body(Body::from("{\"error\":\"Bad Gateway\"}"))
        .unwrap()
}

#[tokio::main]
async fn main() {
    tracing_subscriber::fmt()
        .with_env_filter(
            tracing_subscriber::EnvFilter::try_from_default_env()
                .unwrap_or_else(|_| "api_gateway=info".into()),
        )
        .init();

    let config_path = std::env::var("CONFIG_PATH").unwrap_or_else(|_| "config.yaml".to_string());
    let config = Arc::new(Config::load(&config_path));

    let port = config.server.port;
    let state = AppState { config };

    let app = Router::new()
        .route("/actuator/health", get(health))
        .fallback(any(proxy))
        .with_state(state);

    let addr = SocketAddr::from(([0, 0, 0, 0], port));
    tracing::info!("api-gateway listening on {}", addr);

    let listener = tokio::net::TcpListener::bind(addr).await.unwrap();
    axum::serve(listener, app).await.unwrap();
}
