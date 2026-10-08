use serde::Deserialize;
use std::collections::HashMap;
use std::fs;
use std::path::Path;

#[derive(Debug, Clone, Deserialize)]
pub struct Config {
    pub server: ServerConfig,
    pub services: HashMap<String, Vec<Instance>>,
}

#[derive(Debug, Clone, Deserialize)]
pub struct ServerConfig {
    pub port: u16,
}

#[derive(Debug, Clone, Deserialize)]
pub struct Instance {
    pub host: String,
    pub port: u16,
}

impl Config {
    pub fn load(path: &str) -> Self {
        let content = fs::read_to_string(Path::new(path))
            .unwrap_or_else(|e| panic!("Failed to read config file {}: {}", path, e));
        serde_yaml::from_str(&content)
            .unwrap_or_else(|e| panic!("Failed to parse config file {}: {}", path, e))
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_load_config() {
        let config = Config::load("config.yaml");
        assert!(config.server.port > 0);
        assert!(!config.services.is_empty());
        assert!(config.services.contains_key("authentication-service"));
    }
}
