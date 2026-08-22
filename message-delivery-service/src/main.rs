mod config;
mod consumer;
mod delivery;
mod http;
mod model;

use config::Config;
use tracing_subscriber::EnvFilter;

#[tokio::main]
async fn main() {
    tracing_subscriber::fmt()
        .with_env_filter(EnvFilter::try_from_default_env().unwrap_or_else(|_| EnvFilter::new("info")))
        .init();

    let config = Config::from_env();
    tracing::info!("Starting message-delivery-service");

    let consumer = consumer::create_consumer(&config);

    let consumer_task = tokio::spawn(consumer::run(consumer));
    let health_task = tokio::spawn(http::start_health_server(config.server_port));

    tokio::select! {
        _ = consumer_task => tracing::warn!("Consumer task ended"),
        _ = health_task => tracing::warn!("Health server ended"),
    }
}
