use crate::error::AppError;
use jsonwebtoken::{decode, DecodingKey, Validation};
use serde::Deserialize;

#[derive(Debug, Deserialize)]
struct Claims {
    #[allow(dead_code)]
    sub: String,
    #[serde(rename = "userId")]
    user_id: String,
}

pub fn extract_user_id(token: &str, secret: &str) -> Result<uuid::Uuid, AppError> {
    let token = token.strip_prefix("Bearer ").unwrap_or(token);

    let validation = Validation::default();
    let decoding_key = DecodingKey::from_secret(secret.as_bytes());

    let data = decode::<Claims>(token, &decoding_key, &validation)
        .map_err(|e| AppError::Unauthorized(format!("Invalid token: {}", e)))?;

    uuid::Uuid::parse_str(&data.claims.user_id)
        .map_err(|e| AppError::Unauthorized(format!("Invalid userId in token: {}", e)))
}
