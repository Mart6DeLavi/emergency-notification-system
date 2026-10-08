use crate::config::{Config, ServiceLimit};
use redis::aio::ConnectionManager;
use std::collections::HashMap;
use std::sync::Arc;
use std::time::{SystemTime, UNIX_EPOCH};
use tokio::sync::Mutex;

#[derive(Clone)]
pub struct TokenBucket {
    /// In-memory fallback buckets, keyed by `{service}:{client_ip}`.
    memory: Arc<Mutex<HashMap<String, Bucket>>>,
    redis: Option<ConnectionManager>,
}

#[derive(Clone)]
struct Bucket {
    tokens: f64,
    last_refill: f64,
}

impl TokenBucket {
    pub async fn new(config: &Config) -> Self {
        let redis = match redis::Client::open(config.redis.url.as_str())
            .and_then(|client| Ok(redis::aio::ConnectionManager::new(client)))
        {
            Ok(manager) => match manager.await {
                Ok(m) => Some(m),
                Err(e) => {
                    tracing::warn!("Redis unavailable, using in-memory fallback: {}", e);
                    None
                }
            },
            Err(e) => {
                tracing::warn!("Redis client init failed, using in-memory fallback: {}", e);
                None
            }
        };
        Self {
            memory: Arc::new(Mutex::new(HashMap::new())),
            redis,
        }
    }

    pub async fn check(&self, service: &str, client_ip: &str, limit: &ServiceLimit) -> CheckResult {
        let key = format!("{}:{}", service, client_ip);

        if let Some(redis) = &self.redis {
            match Self::check_redis(redis, &key, limit).await {
                Ok(result) => return result,
                Err(e) => {
                    tracing::warn!("Redis check failed, using in-memory fallback: {}", e);
                }
            }
        }

        self.check_memory(&key, limit).await
    }

    async fn check_redis(
        redis: &ConnectionManager,
        key: &str,
        limit: &ServiceLimit,
    ) -> Result<CheckResult, String> {
        let now = now_seconds();
        let mut conn = redis.clone();

        let script = redis::Script::new(
            r#"
            local key = KEYS[1]
            local capacity = tonumber(ARGV[1])
            local refill = tonumber(ARGV[2])
            local now = tonumber(ARGV[3])

            local bucket = redis.call('HMGET', key, 'tokens', 'last_refill')
            local tokens = tonumber(bucket[1])
            local last_refill = tonumber(bucket[2])

            if tokens == nil then
                tokens = capacity
                last_refill = now
            end

            local elapsed = now - last_refill
            tokens = math.min(capacity, tokens + elapsed * refill)
            last_refill = now

            local allowed = 0
            if tokens >= 1 then
                tokens = tokens - 1
                allowed = 1
            end

            redis.call('HMSET', key, 'tokens', tokens, 'last_refill', last_refill)
            redis.call('EXPIRE', key, 3600)

            return {allowed, tokens}
            "#,
        );

        let result: (i64, f64) = script
            .key(key)
            .arg(limit.capacity)
            .arg(limit.refill_per_second)
            .arg(now)
            .invoke_async(&mut conn)
            .await
            .map_err(|e| e.to_string())?;

        Ok(CheckResult {
            allowed: result.0 == 1,
            remaining: result.1.max(0.0) as u32,
        })
    }

    async fn check_memory(&self, key: &str, limit: &ServiceLimit) -> CheckResult {
        let mut map = self.memory.lock().await;
        let now = now_seconds();

        let bucket = map.entry(key.to_string()).or_insert_with(|| Bucket {
            tokens: limit.capacity as f64,
            last_refill: now,
        });

        let elapsed = now - bucket.last_refill;
        bucket.tokens = (bucket.tokens + elapsed * limit.refill_per_second).min(limit.capacity as f64);
        bucket.last_refill = now;

        let allowed = bucket.tokens >= 1.0;
        if allowed {
            bucket.tokens -= 1.0;
        }

        CheckResult {
            allowed,
            remaining: bucket.tokens.max(0.0) as u32,
        }
    }
}

#[derive(Debug, Clone, serde::Serialize, PartialEq)]
pub struct CheckResult {
    pub allowed: bool,
    pub remaining: u32,
}

fn now_seconds() -> f64 {
    SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .unwrap()
        .as_secs_f64()
}

#[cfg(test)]
mod tests {
    use super::*;

    fn limit(capacity: u32, refill: f64) -> ServiceLimit {
        ServiceLimit {
            capacity,
            refill_per_second: refill,
        }
    }

    #[tokio::test]
    async fn test_memory_bucket_allows_up_to_capacity() {
        let bucket = TokenBucket {
            memory: Arc::new(Mutex::new(HashMap::new())),
            redis: None,
        };
        let limit = limit(3, 1.0);

        assert!(bucket.check("svc", "ip1", &limit).await.allowed);
        assert!(bucket.check("svc", "ip1", &limit).await.allowed);
        assert!(bucket.check("svc", "ip1", &limit).await.allowed);
        assert!(!bucket.check("svc", "ip1", &limit).await.allowed);
    }

    #[tokio::test]
    async fn test_memory_bucket_refills_over_time() {
        let bucket = TokenBucket {
            memory: Arc::new(Mutex::new(HashMap::new())),
            redis: None,
        };
        let limit = limit(1, 1000.0);

        assert!(bucket.check("svc", "ip2", &limit).await.allowed);
        assert!(!bucket.check("svc", "ip2", &limit).await.allowed);

        tokio::time::sleep(std::time::Duration::from_millis(5)).await;
        assert!(bucket.check("svc", "ip2", &limit).await.allowed);
    }

    #[tokio::test]
    async fn test_memory_bucket_isolated_by_key() {
        let bucket = TokenBucket {
            memory: Arc::new(Mutex::new(HashMap::new())),
            redis: None,
        };
        let limit = limit(1, 1.0);

        assert!(bucket.check("svc", "ipA", &limit).await.allowed);
        assert!(bucket.check("svc", "ipB", &limit).await.allowed);
        assert!(!bucket.check("svc", "ipA", &limit).await.allowed);
    }
}
