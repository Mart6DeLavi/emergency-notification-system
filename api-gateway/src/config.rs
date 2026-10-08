use serde::Deserialize;
use std::fs;
use std::path::Path;

#[derive(Debug, Clone, Deserialize)]
pub struct Config {
    pub server: ServerConfig,
    pub rate_limiter: RateLimiterConfig,
    pub load_balancer: LoadBalancerConfig,
    pub routes: Vec<Route>,
}

#[derive(Debug, Clone, Deserialize)]
pub struct ServerConfig {
    pub port: u16,
}

#[derive(Debug, Clone, Deserialize)]
pub struct RateLimiterConfig {
    pub url: String,
}

#[derive(Debug, Clone, Deserialize)]
pub struct LoadBalancerConfig {
    pub url: String,
}

#[derive(Debug, Clone, Deserialize)]
pub struct Route {
    /// URL path prefix to match, e.g. "/api/v1/auth".
    pub path: String,
    /// Service name registered in the load-balancer, e.g. "authentication-service".
    pub service: String,
}

impl Config {
    pub fn load(path: &str) -> Self {
        let content = fs::read_to_string(Path::new(path))
            .unwrap_or_else(|e| panic!("Failed to read config file {}: {}", path, e));
        serde_yaml::from_str(&content)
            .unwrap_or_else(|e| panic!("Failed to parse config file {}: {}", path, e))
    }

    /// Returns the route whose path prefix matches the given path (longest prefix wins).
    pub fn route_for(&self, path: &str) -> Option<&Route> {
        self.routes
            .iter()
            .filter(|r| path.starts_with(r.path.as_str()))
            .max_by_key(|r| r.path.len())
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_load_config() {
        let config = Config::load("config.yaml");
        assert!(config.server.port > 0);
        assert!(!config.routes.is_empty());
    }

    #[test]
    fn test_route_for_longest_prefix() {
        let config = Config::load("config.yaml");
        let route = config.route_for("/api/v1/auth/login").unwrap();
        assert_eq!(route.service, "authentication-service");

        assert!(config.route_for("/unknown/path").is_none());
    }
}
