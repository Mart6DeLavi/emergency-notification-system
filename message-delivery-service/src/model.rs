use serde::Deserialize;
use uuid::Uuid;

#[derive(Debug, Clone, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct DeliveryEvent {
    pub user_id: Uuid,
    pub email: Option<String>,
    pub phone_number: Option<String>,
    pub channel: String,
    pub title: String,
    pub content: String,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum Channel {
    Push,
    Email,
    Sms,
}

impl Channel {
    pub fn from_str(value: &str) -> Option<Self> {
        match value.to_uppercase().as_str() {
            "PUSH" => Some(Self::Push),
            "EMAIL" => Some(Self::Email),
            "SMS" => Some(Self::Sms),
            _ => None,
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_channel_from_str() {
        assert_eq!(Channel::from_str("PUSH"), Some(Channel::Push));
        assert_eq!(Channel::from_str("push"), Some(Channel::Push));
        assert_eq!(Channel::from_str("EMAIL"), Some(Channel::Email));
        assert_eq!(Channel::from_str("sms"), Some(Channel::Sms));
        assert_eq!(Channel::from_str("FAX"), None);
        assert_eq!(Channel::from_str(""), None);
    }

    #[test]
    fn test_delivery_event_deserialization() {
        let json = r#"{
            "userId": "123e4567-e89b-12d3-a456-426614174000",
            "email": "user@example.com",
            "phoneNumber": "+48123456789",
            "channel": "PUSH",
            "title": "Emergency alert",
            "content": "Fire in the building<br>"
        }"#;

        let event: DeliveryEvent = serde_json::from_str(json).unwrap();

        assert_eq!(
            event.user_id.to_string(),
            "123e4567-e89b-12d3-a456-426614174000"
        );
        assert_eq!(event.email.as_deref(), Some("user@example.com"));
        assert_eq!(event.phone_number.as_deref(), Some("+48123456789"));
        assert_eq!(event.channel, "PUSH");
        assert_eq!(event.title, "Emergency alert");
        assert_eq!(event.content, "Fire in the building<br>");
    }

    #[test]
    fn test_delivery_event_nullable_fields() {
        let json = r#"{
            "userId": "123e4567-e89b-12d3-a456-426614174000",
            "email": null,
            "phoneNumber": null,
            "channel": "SMS",
            "title": "Alert",
            "content": "Test"
        }"#;

        let event: DeliveryEvent = serde_json::from_str(json).unwrap();

        assert!(event.email.is_none());
        assert!(event.phone_number.is_none());
        assert_eq!(Channel::from_str(&event.channel), Some(Channel::Sms));
    }
}
