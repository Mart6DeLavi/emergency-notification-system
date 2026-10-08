use crate::bucket::{CheckResult, TokenBucket};
use crate::config::Config;
use axum::extract::State;
use axum::routing::{get, post};
use axum::{Json, Router};
use serde::{Deserialize, Serialize};
use std::net::SocketAddr;
use std::sync::Arc;

mod bucket;
mod config;

#[derive(Clone)]
struct AppState {
    config: Arc<Config>,
    bucket: TokenBucket,
}

#[derive(Deserialize)]
struct CheckRequest {
    #[serde(rename = "clientIp")]
    client_ip: String,
    service: String,
}

#[derive(Serialize)]
struct HealthResponse {
    status: &'static str,
}

async fn check(
    State(state): State<AppState>,
    Json(req): Json<CheckRequest>,
) -> Json<CheckResult> {
    let limit = state.config.limit_for(&req.service);
    let result = state.bucket.check(&req.service, &req.client_ip, &limit).await;
    Json(result)
}

async fn health() -> Json<HealthResponse> {
    Json(HealthResponse { status: "UP" })
}

#[tokio::main]
async fn main() {
    tracing_subscriber::fmt()
        .with_env_filter(
            tracing_subscriber::EnvFilter::try_from_default_env()
                .unwrap_or_else(|_| "rate_limiter_service=info".into()),
        )
        .init();

    let config_path = std::env::var("CONFIG_PATH").unwrap_or_else(|_| "config.yaml".to_string());
    let config = Arc::new(Config::load(&config_path));

    let bucket = TokenBucket::new(&config).await;

    let port = config.server.port;
    let state = AppState { config, bucket };

    let app = Router::new()
        .route("/api/v1/check", post(check))
        .route("/actuator/health", get(health))
        .with_state(state);

    let addr = SocketAddr::from(([0, 0, 0, 0], port));
    tracing::info!("rate-limiter-service listening on {}", addr);

    let listener = tokio::net::TcpListener::bind(addr).await.unwrap();
    axum::serve(listener, app).await.unwrap();
}
