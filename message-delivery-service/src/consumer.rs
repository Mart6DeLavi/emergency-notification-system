use crate::delivery::deliver;
use crate::model::DeliveryEvent;
use crate::Config;
use rdkafka::config::ClientConfig;
use rdkafka::consumer::{CommitMode, Consumer, StreamConsumer};
use rdkafka::Message;
use tracing::{error, info};

pub fn create_consumer(config: &Config) -> StreamConsumer {
    let consumer: StreamConsumer = ClientConfig::new()
        .set("group.id", &config.consumer_group)
        .set("bootstrap.servers", &config.kafka_brokers)
        .set("enable.auto.commit", "true")
        .set("auto.offset.reset", "earliest")
        .create()
        .expect("Failed to create Kafka consumer");

    consumer
        .subscribe(&[&config.delivery_topic])
        .expect("Failed to subscribe to topic");

    info!("Subscribed to topic: {}", config.delivery_topic);
    consumer
}

pub async fn run(consumer: StreamConsumer) {
    info!("Message delivery consumer started");

    loop {
        match consumer.recv().await {
            Ok(message) => {
                let payload = match message.payload() {
                    Some(p) => p,
                    None => continue,
                };

                match serde_json::from_slice::<DeliveryEvent>(payload) {
                    Ok(event) => {
                        info!(
                            "Received delivery event: userId={} channel={}",
                            event.user_id, event.channel
                        );
                        deliver(&event);
                    }
                    Err(e) => {
                        error!("Failed to deserialize delivery event: {}", e);
                    }
                }

                if let Err(e) = consumer.commit_message(&message, CommitMode::Async) {
                    error!("Failed to commit offset: {}", e);
                }
            }
            Err(e) => {
                error!("Kafka consumer error: {}", e);
                tokio::time::sleep(std::time::Duration::from_secs(1)).await;
            }
        }
    }
}
