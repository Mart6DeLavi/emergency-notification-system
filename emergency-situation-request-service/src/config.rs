use std::env;

#[derive(Debug, Clone)]
pub struct Config {
    pub database_url: String,
    pub kafka_brokers: String,
    pub jwt_secret: String,
    pub server_port: u16,
}

impl Config {
    pub fn from_env() -> Self {
        Config {
            database_url: env::var("DATABASE_URL")
                .unwrap_or_else(|_| "postgres://postgres:postgres@localhost:5432/Sensa_Emergency_Service".to_string()),
            kafka_brokers: env::var("KAFKA_BROKERS")
                .unwrap_or_else(|_| "localhost:9092".to_string()),
            jwt_secret: env::var("JWT_SECRET")
                .unwrap_or_else(|_| "change-me".to_string()),
            server_port: env::var("SERVER_PORT")
                .unwrap_or_else(|_| "8006".to_string())
                .parse()
                .unwrap_or(8006),
        }
    }
}
