use crate::config::Config;
use crate::model::{Channel, DeliveryEvent};
use lettre::message::header::ContentType;
use lettre::transport::smtp::authentication::Credentials;
use lettre::{AsyncSmtpTransport, AsyncTransport, Message, Tokio1Executor};
use tracing::{info, warn};

pub async fn deliver(event: &DeliveryEvent, config: &Config) {
    let channel = match Channel::from_str(&event.channel) {
        Some(c) => c,
        None => {
            warn!("Unknown delivery channel: {}", event.channel);
            return;
        }
    };

    let result = match channel {
        Channel::Push => deliver_push(event, config).await,
        Channel::Email => deliver_email(event, config).await,
        Channel::Sms => deliver_sms(event, config).await,
    };

    if let Err(e) = result {
        warn!(
            "Failed to deliver {} for userId={}: {}",
            event.channel, event.user_id, e
        );
    }
}

async fn deliver_push(event: &DeliveryEvent, config: &Config) -> Result<(), String> {
    let token = match event.device_token.as_deref() {
        Some(t) => t,
        None => {
            info!("No device token for userId={}, skipping PUSH", event.user_id);
            return Ok(());
        }
    };

    let api_key = match config.fcm_api_key.as_deref() {
        Some(k) => k,
        None => {
            info!("FCM not configured, logging PUSH delivery for userId={}", event.user_id);
            info!(
                "PUSH delivered to token={} title=\"{}\" content_len={}",
                token,
                event.title,
                event.content.len()
            );
            return Ok(());
        }
    };

    let project_id = config.fcm_project_id.as_deref().unwrap_or("sensa");
    let url = format!(
        "https://fcm.googleapis.com/v1/projects/{}/messages:send",
        project_id
    );

    let body = serde_json::json!({
        "message": {
            "token": token,
            "notification": {
                "title": event.title,
                "body": event.content
            }
        }
    });

    let client = reqwest::Client::new();
    let response = client
        .post(&url)
        .bearer_auth(api_key)
        .json(&body)
        .send()
        .await
        .map_err(|e| e.to_string())?;

    if response.status().is_success() {
        info!("PUSH delivered to userId={} via FCM", event.user_id);
        Ok(())
    } else {
        Err(format!("FCM responded with status {}", response.status()))
    }
}

async fn deliver_email(event: &DeliveryEvent, config: &Config) -> Result<(), String> {
    let email = match event.email.as_deref() {
        Some(e) => e,
        None => {
            info!("No email for userId={}, skipping EMAIL", event.user_id);
            return Ok(());
        }
    };

    let host = match config.smtp_host.as_deref() {
        Some(h) => h,
        None => {
            info!("SMTP not configured, logging EMAIL delivery for {}", email);
            info!(
                "EMAIL delivered to {} (userId={}) title=\"{}\"",
                email, event.user_id, event.title
            );
            return Ok(());
        }
    };

    let from = config
        .smtp_from
        .clone()
        .unwrap_or_else(|| "no-reply@sensa.local".to_string());

    let message = Message::builder()
        .from(from.parse::<lettre::message::Mailbox>().map_err(|e| e.to_string())?)
        .to(email.parse::<lettre::message::Mailbox>().map_err(|e| e.to_string())?)
        .subject(event.title.clone())
        .header(ContentType::TEXT_HTML)
        .body(event.content.clone())
        .map_err(|e| e.to_string())?;

    let mut builder = if config.smtp_tls {
        AsyncSmtpTransport::<Tokio1Executor>::relay(host)
            .map_err(|e| e.to_string())?
    } else {
        AsyncSmtpTransport::<Tokio1Executor>::builder_dangerous(host)
    }
    .port(config.smtp_port);

    if let (Some(username), Some(password)) = (config.smtp_username.as_deref(), config.smtp_password.as_deref()) {
        builder = builder.credentials(Credentials::new(username.to_string(), password.to_string()));
    }

    let mailer = builder.build();
    mailer.send(message).await.map_err(|e| e.to_string())?;

    info!("EMAIL delivered to {} (userId={})", email, event.user_id);
    Ok(())
}

async fn deliver_sms(event: &DeliveryEvent, config: &Config) -> Result<(), String> {
    let phone = match event.phone_number.as_deref() {
        Some(p) => p,
        None => {
            info!("No phone for userId={}, skipping SMS", event.user_id);
            return Ok(());
        }
    };

    let (access_key, secret_key, region) = match (
        config.aws_access_key_id.as_deref(),
        config.aws_secret_access_key.as_deref(),
        config.aws_region.as_deref(),
    ) {
        (Some(a), Some(s), Some(r)) => (a, s, r),
        _ => {
            info!("AWS SNS not configured, logging SMS delivery for {}", phone);
            info!(
                "SMS delivered to {} (userId={}) title=\"{}\"",
                phone, event.user_id, event.title
            );
            return Ok(());
        }
    };

    let credentials = aws_credential_types::Credentials::new(
        access_key,
        secret_key,
        None,
        None,
        "message-delivery-service",
    );

    let provider = aws_credential_types::provider::SharedCredentialsProvider::new(credentials);

    let sdk_config = aws_config::SdkConfig::builder()
        .region(aws_config::Region::new(region.to_string()))
        .credentials_provider(provider)
        .build();

    let client = aws_sdk_sns::Client::new(&sdk_config);

    let message = format!("{}: {}", event.title, event.content);
    client
        .publish()
        .phone_number(phone)
        .message(message)
        .send()
        .await
        .map_err(|e| e.to_string())?;

    info!("SMS delivered to {} (userId={}) via AWS SNS", phone, event.user_id);
    Ok(())
}
