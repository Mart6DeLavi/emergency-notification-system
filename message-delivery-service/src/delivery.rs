use crate::model::{Channel, DeliveryEvent};
use tracing::{info, warn};

pub fn deliver(event: &DeliveryEvent) {
    let channel = match Channel::from_str(&event.channel) {
        Some(c) => c,
        None => {
            warn!("Unknown delivery channel: {}", event.channel);
            return;
        }
    };

    match channel {
        Channel::Push => deliver_push(event),
        Channel::Email => deliver_email(event),
        Channel::Sms => deliver_sms(event),
    }
}

fn deliver_push(event: &DeliveryEvent) {
    // TODO: send push notification via FCM / APNs
    info!(
        "PUSH delivered to userId={} title=\"{}\" content_len={}",
        event.user_id,
        event.title,
        event.content.len()
    );
}

fn deliver_email(event: &DeliveryEvent) {
    // TODO: send email via Amazon SES
    let email = event.email.as_deref().unwrap_or("(no email)");
    info!(
        "EMAIL delivered to {} (userId={}) title=\"{}\"",
        email, event.user_id, event.title
    );
}

fn deliver_sms(event: &DeliveryEvent) {
    // TODO: send SMS via Twilio
    let phone = event.phone_number.as_deref().unwrap_or("(no phone)");
    info!(
        "SMS delivered to {} (userId={}) title=\"{}\"",
        phone, event.user_id, event.title
    );
}
