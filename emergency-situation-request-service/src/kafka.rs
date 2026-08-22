use rdkafka::config::ClientConfig;
use rdkafka::producer::{FutureProducer, FutureRecord};
use rdkafka::util::Timeout;
use serde::Serialize;
use std::time::Duration;

#[derive(Clone)]
pub struct KafkaProducer {
    producer: FutureProducer,
    topic: String,
}

impl KafkaProducer {
    pub fn new(brokers: &str, topic: &str) -> Self {
        let producer: FutureProducer = ClientConfig::new()
            .set("bootstrap.servers", brokers)
            .set("message.timeout.ms", "5000")
            .create()
            .expect("Failed to create Kafka producer");

        KafkaProducer {
            producer,
            topic: topic.to_string(),
        }
    }

    pub async fn send<T: Serialize>(&self, key: &str, value: &T) -> Result<(), String> {
        let payload = serde_json::to_string(value).map_err(|e| e.to_string())?;

        let record = FutureRecord::to(&self.topic).key(key).payload(&payload);

        self.producer
            .send(record, Timeout::After(Duration::from_secs(5)))
            .await
            .map_err(|(e, _)| format!("Kafka send failed: {}", e))?;

        Ok(())
    }
}
