use axum::{routing::get, Json, Router};
use axum_prometheus::PrometheusMetricLayer;
use serde_json::{json, Value};
use std::net::SocketAddr;

async fn health() -> Json<Value> {
    Json(json!({ "status": "UP" }))
}

pub async fn start_health_server(port: u16) {
    let (prometheus_layer, metric_handle) = PrometheusMetricLayer::pair();
    let metrics = metric_handle.clone();

    let app = Router::new()
        .route("/actuator/health", get(health))
        .route("/metrics", get(move || { let m = metrics.clone(); async move { m.render() } }))
        .layer(prometheus_layer);
    let addr = SocketAddr::from(([0, 0, 0, 0], port));

    tracing::info!("Health server listening on {}", addr);

    let listener = tokio::net::TcpListener::bind(addr)
        .await
        .expect("Failed to bind health server");
    axum::serve(listener, app).await.expect("Health server failed");
}
