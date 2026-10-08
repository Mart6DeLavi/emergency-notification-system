use crate::balancer::LoadBalancer;
use crate::config::Config;
use axum::extract::{Path, State};
use axum::http::StatusCode;
use axum::routing::get;
use axum::{Json, Router};
use serde::Serialize;
use std::net::SocketAddr;
use std::sync::Arc;

mod balancer;
mod config;

#[derive(Clone)]
struct AppState {
    load_balancer: Arc<LoadBalancer>,
}

#[derive(Serialize)]
struct InstanceResponse {
    service: String,
    host: String,
    port: u16,
}

#[derive(Serialize)]
struct HealthResponse {
    status: &'static str,
}

async fn get_instance(
    State(state): State<AppState>,
    Path(service): Path<String>,
) -> Result<Json<InstanceResponse>, StatusCode> {
    match state.load_balancer.next_instance(&service) {
        Some(instance) => Ok(Json(InstanceResponse {
            service,
            host: instance.host.clone(),
            port: instance.port,
        })),
        None => Err(StatusCode::NOT_FOUND),
    }
}

async fn health() -> Json<HealthResponse> {
    Json(HealthResponse { status: "UP" })
}

#[tokio::main]
async fn main() {
    tracing_subscriber::fmt()
        .with_env_filter(
            tracing_subscriber::EnvFilter::try_from_default_env()
                .unwrap_or_else(|_| "load_balancer_service=info".into()),
        )
        .init();

    let config_path = std::env::var("CONFIG_PATH").unwrap_or_else(|_| "config.yaml".to_string());
    let config = Config::load(&config_path);

    let load_balancer = Arc::new(LoadBalancer::new(&config));

    let state = AppState { load_balancer };

    let app = Router::new()
        .route("/api/v1/instances/{service}", get(get_instance))
        .route("/actuator/health", get(health))
        .with_state(state);

    let addr = SocketAddr::from(([0, 0, 0, 0], config.server.port));
    tracing::info!("load-balancer-service listening on {}", addr);

    let listener = tokio::net::TcpListener::bind(addr).await.unwrap();
    axum::serve(listener, app).await.unwrap();
}
