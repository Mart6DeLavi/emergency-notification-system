use std::env;

#[derive(Debug, Clone)]
pub struct Config {
    pub mongo_uri: String,
    pub server_port: u16,
    pub jwt_secret: String,
    pub k6_bin: String,
    pub k6_script: String,
    pub api_base_url: String,
    pub test_email: String,
    pub test_password: String,
}

impl Config {
    pub fn from_env() -> Self {
        Self {
            mongo_uri: env::var("MONGO_URI")
                .unwrap_or_else(|_| "mongodb://localhost:27017".to_string()),
            server_port: env::var("SERVER_PORT")
                .ok()
                .and_then(|v| v.parse().ok())
                .unwrap_or(8010),
            jwt_secret: env::var("JWT_USER_SECRET").unwrap_or_else(|_| "change-me".to_string()),
            k6_bin: env::var("K6_BIN").unwrap_or_else(|_| "k6".to_string()),
            k6_script: env::var("K6_SCRIPT").unwrap_or_else(|_| "k6/scenario.js".to_string()),
            api_base_url: env::var("API_BASE_URL")
                .unwrap_or_else(|_| "http://api-gateway:8007".to_string()),
            test_email: env::var("TEST_EMAIL")
                .unwrap_or_else(|_| "ios-test4@sensanotify.com".to_string()),
            test_password: env::var("TEST_PASSWORD")
                .unwrap_or_else(|_| "TestPass123!".to_string()),
        }
    }
}
