mod auth;
mod config;
mod error;
mod handlers;
mod kafka;
mod model;
mod repository;

use axum::routing::{get, patch, post};
use axum::Router;
use config::Config;
use handlers::AppState;
use kafka::KafkaProducer;
use repository::EmergencyRepository;
use sqlx::postgres::PgPoolOptions;
use std::net::SocketAddr;

#[tokio::main]
async fn main() {
    tracing_subscriber::fmt()
        .with_env_filter(
            tracing_subscriber::EnvFilter::try_from_default_env()
                .unwrap_or_else(|_| "emergency_situation_request_service=info".into()),
        )
        .init();

    let config = Config::from_env();

    let pool = PgPoolOptions::new()
        .max_connections(5)
        .connect(&config.database_url)
        .await
        .expect("Failed to connect to PostgreSQL");

    sqlx::migrate!("./migrations")
        .run(&pool)
        .await
        .expect("Failed to run migrations");

    let repository = EmergencyRepository::new(pool);
    let kafka = KafkaProducer::new(&config.kafka_brokers, "emergency.confirmed");

    let state = AppState {
        repository,
        kafka,
        config: config.clone(),
    };

    let app = Router::new()
        .route("/api/v1/emergencies", post(handlers::create_emergency))
        .route("/api/v1/emergencies", get(handlers::list_emergencies))
        .route("/api/v1/emergencies/{id}", get(handlers::get_emergency))
        .route("/api/v1/emergencies/{id}/status", patch(handlers::update_status))
        .route("/actuator/health", get(|| async { "OK" }))
        .with_state(state);

    let addr = SocketAddr::from(([0, 0, 0, 0], config.server_port));
    tracing::info!("Listening on {}", addr);

    let listener = tokio::net::TcpListener::bind(addr).await.unwrap();
    axum::serve(listener, app).await.unwrap();
}
