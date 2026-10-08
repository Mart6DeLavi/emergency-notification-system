use crate::config::{Config, Instance};
use std::collections::HashMap;
use std::sync::atomic::{AtomicUsize, Ordering};
use std::sync::Arc;

#[derive(Clone)]
pub struct LoadBalancer {
    services: Arc<HashMap<String, Vec<Instance>>>,
    counters: Arc<HashMap<String, AtomicUsize>>,
}

impl LoadBalancer {
    pub fn new(config: &Config) -> Self {
        let counters = config
            .services
            .keys()
            .map(|name| (name.clone(), AtomicUsize::new(0)))
            .collect();
        Self {
            services: Arc::new(config.services.clone()),
            counters: Arc::new(counters),
        }
    }

    pub fn next_instance(&self, service: &str) -> Option<&Instance> {
        let instances = self.services.get(service)?;
        if instances.is_empty() {
            return None;
        }

        let counter = self.counters.get(service)?;
        let index = counter.fetch_add(1, Ordering::Relaxed) % instances.len();
        instances.get(index)
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::config::Config;

    fn test_config() -> Config {
        Config {
            server: crate::config::ServerConfig { port: 8008 },
            services: {
                let mut map = HashMap::new();
                map.insert(
                    "test-service".to_string(),
                    vec![
                        Instance { host: "a".into(), port: 1 },
                        Instance { host: "b".into(), port: 2 },
                        Instance { host: "c".into(), port: 3 },
                    ],
                );
                map
            },
        }
    }

    #[test]
    fn test_round_robin_cycles_through_instances() {
        let config = test_config();
        let lb = LoadBalancer::new(&config);

        let first = lb.next_instance("test-service").unwrap();
        let second = lb.next_instance("test-service").unwrap();
        let third = lb.next_instance("test-service").unwrap();
        let fourth = lb.next_instance("test-service").unwrap();

        assert_eq!(first.host, "a");
        assert_eq!(second.host, "b");
        assert_eq!(third.host, "c");
        assert_eq!(fourth.host, "a");
    }

    #[test]
    fn test_unknown_service_returns_none() {
        let config = test_config();
        let lb = LoadBalancer::new(&config);
        assert!(lb.next_instance("missing").is_none());
    }
}
