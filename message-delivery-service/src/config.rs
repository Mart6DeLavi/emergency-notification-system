use std::env;

#[derive(Clone, Debug)]
pub struct Config {
    pub kafka_brokers: String,
    pub consumer_group: String,
    pub delivery_topic: String,
    pub server_port: u16,
}

impl Config {
    pub fn from_env() -> Self {
        Self {
            kafka_brokers: env::var("KAFKA_BROKERS")
                .unwrap_or_else(|_| "localhost:9092".to_string()),
            consumer_group: env::var("CONSUMER_GROUP")
                .unwrap_or_else(|_| "message-delivery-service".to_string()),
            delivery_topic: env::var("DELIVERY_TOPIC")
                .unwrap_or_else(|_| "notification.delivery".to_string()),
            server_port: env::var("SERVER_PORT")
                .unwrap_or_else(|_| "8005".to_string())
                .parse()
                .unwrap_or(8005),
        }
    }
}
