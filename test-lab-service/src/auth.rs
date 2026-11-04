use axum::http::HeaderMap;
use jsonwebtoken::{decode, Algorithm, DecodingKey, Validation};
use serde::Deserialize;

#[derive(Debug, Deserialize)]
struct Claims {
    #[allow(dead_code)]
    sub: Option<String>,
    #[serde(rename = "userId")]
    user_id: Option<String>,
}

pub fn extract_user_id(headers: &HeaderMap, secret: &str) -> Result<String, String> {
    let token = headers
        .get("Authorization")
        .and_then(|v| v.to_str().ok())
        .ok_or_else(|| "Missing Authorization header".to_string())?
        .trim_start_matches("Bearer ")
        .trim();

    let mut validation = Validation::new(Algorithm::HS512);
    validation.validate_exp = true;

    let data = decode::<Claims>(
        token,
        &DecodingKey::from_secret(secret.as_bytes()),
        &validation,
    )
    .map_err(|e| format!("Invalid token: {}", e))?;

    data.claims
        .user_id
        .or(data.claims.sub)
        .ok_or_else(|| "Token has no user id".to_string())
}
