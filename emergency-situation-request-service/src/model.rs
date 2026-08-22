use serde::{Deserialize, Serialize};
use sqlx::FromRow;
use uuid::Uuid;

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq, Eq)]
#[serde(rename_all = "SCREAMING_SNAKE_CASE")]
pub enum EmergencyStatus {
    Reported,
    UnderReview,
    Confirmed,
    Broadcast,
    Rejected,
}

impl EmergencyStatus {
    pub fn as_str(&self) -> &'static str {
        match self {
            EmergencyStatus::Reported => "REPORTED",
            EmergencyStatus::UnderReview => "UNDER_REVIEW",
            EmergencyStatus::Confirmed => "CONFIRMED",
            EmergencyStatus::Broadcast => "BROADCAST",
            EmergencyStatus::Rejected => "REJECTED",
        }
    }

    pub fn from_str(s: &str) -> Result<Self, String> {
        match s {
            "REPORTED" => Ok(EmergencyStatus::Reported),
            "UNDER_REVIEW" => Ok(EmergencyStatus::UnderReview),
            "CONFIRMED" => Ok(EmergencyStatus::Confirmed),
            "BROADCAST" => Ok(EmergencyStatus::Broadcast),
            "REJECTED" => Ok(EmergencyStatus::Rejected),
            other => Err(format!("Unknown status: {}", other)),
        }
    }

    /// Valid transitions for the verification workflow.
    pub fn can_transit_to(&self, next: &EmergencyStatus) -> bool {
        matches!(
            (self, next),
            (EmergencyStatus::Reported, EmergencyStatus::UnderReview)
                | (EmergencyStatus::Reported, EmergencyStatus::Rejected)
                | (EmergencyStatus::UnderReview, EmergencyStatus::Confirmed)
                | (EmergencyStatus::UnderReview, EmergencyStatus::Rejected)
                | (EmergencyStatus::Confirmed, EmergencyStatus::Broadcast)
                | (EmergencyStatus::Confirmed, EmergencyStatus::Rejected)
                | (EmergencyStatus::Broadcast, EmergencyStatus::Rejected)
        )
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_status_transitions() {
        assert!(EmergencyStatus::Reported.can_transit_to(&EmergencyStatus::UnderReview));
        assert!(EmergencyStatus::Reported.can_transit_to(&EmergencyStatus::Rejected));
        assert!(EmergencyStatus::UnderReview.can_transit_to(&EmergencyStatus::Confirmed));
        assert!(EmergencyStatus::UnderReview.can_transit_to(&EmergencyStatus::Rejected));
        assert!(EmergencyStatus::Confirmed.can_transit_to(&EmergencyStatus::Broadcast));
        assert!(EmergencyStatus::Confirmed.can_transit_to(&EmergencyStatus::Rejected));
        assert!(EmergencyStatus::Broadcast.can_transit_to(&EmergencyStatus::Rejected));
    }

    #[test]
    fn test_invalid_transitions() {
        assert!(!EmergencyStatus::Reported.can_transit_to(&EmergencyStatus::Confirmed));
        assert!(!EmergencyStatus::Reported.can_transit_to(&EmergencyStatus::Broadcast));
        assert!(!EmergencyStatus::UnderReview.can_transit_to(&EmergencyStatus::Reported));
        assert!(!EmergencyStatus::Broadcast.can_transit_to(&EmergencyStatus::Confirmed));
        assert!(!EmergencyStatus::Rejected.can_transit_to(&EmergencyStatus::Reported));
    }

    #[test]
    fn test_status_str_roundtrip() {
        assert_eq!(EmergencyStatus::from_str("REPORTED").unwrap(), EmergencyStatus::Reported);
        assert_eq!(EmergencyStatus::from_str("CONFIRMED").unwrap(), EmergencyStatus::Confirmed);
        assert_eq!(EmergencyStatus::Confirmed.as_str(), "CONFIRMED");
        assert!(EmergencyStatus::from_str("INVALID").is_err());
    }
}

#[derive(Debug, Clone, Serialize, Deserialize, FromRow)]
pub struct Emergency {
    pub id: i64,
    pub user_id: Uuid,
    pub title: String,
    pub description: Option<String>,
    pub files: Option<serde_json::Value>,
    pub country: Option<String>,
    pub city: Option<String>,
    pub street: Option<String>,
    pub alarm_timestamp: Option<chrono::DateTime<chrono::Utc>>,
    pub status: String,
    pub created_at: chrono::DateTime<chrono::Utc>,
}
