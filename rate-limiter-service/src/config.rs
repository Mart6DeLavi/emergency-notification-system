use serde::Deserialize;
use std::collections::HashMap;
use std::fs;
use std::path::Path;

#[derive(Debug, Clone, Deserialize)]
pub struct Config {
    pub server: ServerConfig,
    pub redis: RedisConfig,
    pub limits: HashMap<String, ServiceLimit>,
}

#[derive(Debug, Clone, Deserialize)]
pub struct ServerConfig {
    pub port: u16,
}

#[derive(Debug, Clone, Deserialize)]
pub struct RedisConfig {
    pub url: String,
}

#[derive(Debug, Clone, Deserialize)]
pub struct ServiceLimit {
    /// Maximum number of tokens the bucket can hold.
    pub capacity: u32,
    /// Tokens refilled per second.
    pub refill_per_second: f64,
}

impl Config {
    pub fn load(path: &str) -> Self {
        let content = fs::read_to_string(Path::new(path))
            .unwrap_or_else(|e| panic!("Failed to read config file {}: {}", path, e));
        serde_yaml::from_str(&content)
            .unwrap_or_else(|e| panic!("Failed to parse config file {}: {}", path, e))
    }

    pub fn limit_for(&self, service: &str) -> ServiceLimit {
        self.limits
            .get(service)
            .cloned()
            .unwrap_or_else(|| ServiceLimit {
                capacity: 10,
                refill_per_second: 1.0,
            })
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_load_config() {
        let config = Config::load("config.yaml");
        assert!(config.server.port > 0);
        assert!(config.limits.contains_key("authentication-service"));
    }

    #[test]
    fn test_limit_fallback_default() {
        let config = Config::load("config.yaml");
        let limit = config.limit_for("unknown-service");
        assert_eq!(limit.capacity, 10);
    }
}
