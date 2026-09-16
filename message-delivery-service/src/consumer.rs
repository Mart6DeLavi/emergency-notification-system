use crate::config::Config;
use crate::delivery::deliver;
use crate::model::DeliveryEvent;
use futures_util::StreamExt;
use lapin::options::{
    BasicAckOptions, BasicConsumeOptions, QueueDeclareOptions,
};
use lapin::types::FieldTable;
use lapin::{Connection, ConnectionProperties};
use tracing::{error, info};

pub async fn run(config: Config) {
    let mut retry_delay = 1u64;

    loop {
        match consume_loop(&config).await {
            Ok(()) => {
                error!("RabbitMQ consumer loop ended unexpectedly, reconnecting...");
            }
            Err(e) => {
                error!("RabbitMQ consumer error: {}", e);
            }
        }

        info!("Reconnecting in {}s...", retry_delay);
        tokio::time::sleep(std::time::Duration::from_secs(retry_delay)).await;
        retry_delay = (retry_delay * 2).min(60);
    }
}

async fn consume_loop(config: &Config) -> Result<(), String> {
    let connection = Connection::connect(&config.rabbitmq_url, ConnectionProperties::default())
        .await
        .map_err(|e| e.to_string())?;

    let channel = connection.create_channel().await.map_err(|e| e.to_string())?;

    channel
        .queue_declare(
            &config.delivery_queue,
            QueueDeclareOptions {
                durable: true,
                ..QueueDeclareOptions::default()
            },
            FieldTable::default(),
        )
        .await
        .map_err(|e| e.to_string())?;

    let mut consumer = channel
        .basic_consume(
            &config.delivery_queue,
            "message-delivery-service",
            BasicConsumeOptions::default(),
            FieldTable::default(),
        )
        .await
        .map_err(|e| e.to_string())?;

    info!("Consuming queue: {}", config.delivery_queue);

    while let Some(delivery) = consumer.next().await {
        let delivery = match delivery {
            Ok(d) => d,
            Err(e) => {
                error!("Failed to receive message: {}", e);
                continue;
            }
        };

        match serde_json::from_slice::<DeliveryEvent>(&delivery.data) {
            Ok(event) => {
                info!(
                    "Received delivery event: userId={} channel={}",
                    event.user_id, event.channel
                );
                deliver(&event, config).await;
            }
            Err(e) => {
                error!("Failed to deserialize delivery event: {}", e);
            }
        }

        if let Err(e) = delivery.ack(BasicAckOptions::default()).await {
            error!("Failed to ack message: {}", e);
        }
    }

    Ok(())
}
