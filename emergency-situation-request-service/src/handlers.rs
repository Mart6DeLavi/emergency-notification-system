use crate::auth::extract_user_id;
use crate::config::Config;
use crate::error::AppError;
use crate::kafka::KafkaProducer;
use crate::model::{Emergency, EmergencyStatus};
use crate::repository::EmergencyRepository;
use axum::extract::{Path, State};
use axum::http::HeaderMap;
use axum::Json;
use serde::{Deserialize, Serialize};
use std::collections::HashMap;

#[derive(Clone)]
pub struct AppState {
    pub repository: EmergencyRepository,
    pub kafka: KafkaProducer,
    pub config: Config,
}

#[derive(Debug, Deserialize)]
pub struct CreateEmergencyRequest {
    pub title: String,
    pub description: Option<String>,
    pub files: Option<serde_json::Value>,
    pub country: Option<String>,
    pub city: Option<String>,
    pub street: Option<String>,
    pub alarm_timestamp: Option<chrono::DateTime<chrono::Utc>>,
}

#[derive(Debug, Deserialize)]
pub struct UpdateStatusRequest {
    pub status: String,
}

#[derive(Debug, Serialize)]
pub struct EmergencyResponse {
    pub id: i64,
    pub user_id: uuid::Uuid,
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

impl From<Emergency> for EmergencyResponse {
    fn from(e: Emergency) -> Self {
        EmergencyResponse {
            id: e.id,
            user_id: e.user_id,
            title: e.title,
            description: e.description,
            files: e.files,
            country: e.country,
            city: e.city,
            street: e.street,
            alarm_timestamp: e.alarm_timestamp,
            status: e.status,
            created_at: e.created_at,
        }
    }
}

#[derive(Debug, Serialize)]
pub struct EmergencyConfirmedEvent {
    pub emergency_id: i64,
    pub title: String,
    pub description: Option<String>,
    pub city: Option<String>,
    pub street: Option<String>,
    pub country: Option<String>,
    pub template_name: String,
    pub template_data: HashMap<String, String>,
}

pub async fn create_emergency(
    State(state): State<AppState>,
    headers: HeaderMap,
    Json(req): Json<CreateEmergencyRequest>,
) -> Result<(axum::http::StatusCode, Json<EmergencyResponse>), AppError> {
    let user_id = auth_user_id(&headers, &state.config)?;

    let emergency = state
        .repository
        .create(
            user_id,
            &req.title,
            req.description.as_deref(),
            req.files,
            req.country.as_deref(),
            req.city.as_deref(),
            req.street.as_deref(),
            req.alarm_timestamp,
        )
        .await?;

    Ok((
        axum::http::StatusCode::CREATED,
        Json(EmergencyResponse::from(emergency)),
    ))
}

pub async fn list_emergencies(
    State(state): State<AppState>,
    headers: HeaderMap,
) -> Result<Json<Vec<EmergencyResponse>>, AppError> {
    auth_user_id(&headers, &state.config)?;
    let emergencies = state.repository.find_all().await?;
    Ok(Json(emergencies.into_iter().map(EmergencyResponse::from).collect()))
}

pub async fn get_emergency(
    State(state): State<AppState>,
    headers: HeaderMap,
    Path(id): Path<i64>,
) -> Result<Json<EmergencyResponse>, AppError> {
    auth_user_id(&headers, &state.config)?;
    let emergency = state
        .repository
        .find_by_id(id)
        .await?
        .ok_or_else(|| AppError::NotFound(format!("Emergency {} not found", id)))?;
    Ok(Json(EmergencyResponse::from(emergency)))
}

pub async fn update_status(
    State(state): State<AppState>,
    headers: HeaderMap,
    Path(id): Path<i64>,
    Json(req): Json<UpdateStatusRequest>,
) -> Result<Json<EmergencyResponse>, AppError> {
    auth_user_id(&headers, &state.config)?;

    let current = state
        .repository
        .find_by_id(id)
        .await?
        .ok_or_else(|| AppError::NotFound(format!("Emergency {} not found", id)))?;

    let current_status = EmergencyStatus::from_str(&current.status)
        .map_err(AppError::BadRequest)?;
    let next_status = EmergencyStatus::from_str(&req.status).map_err(AppError::BadRequest)?;

    if !current_status.can_transit_to(&next_status) {
        return Err(AppError::Conflict(format!(
            "Invalid status transition: {} -> {}",
            current_status.as_str(),
            next_status.as_str()
        )));
    }

    let updated = state
        .repository
        .update_status(id, next_status.as_str())
        .await?
        .ok_or_else(|| AppError::NotFound(format!("Emergency {} not found", id)))?;

    if next_status == EmergencyStatus::Confirmed {
        emit_confirmed_event(&state, &updated).await?;
    }

    Ok(Json(EmergencyResponse::from(updated)))
}

async fn emit_confirmed_event(state: &AppState, emergency: &Emergency) -> Result<(), AppError> {
    let mut template_data = HashMap::new();
    template_data.insert("title".to_string(), emergency.title.clone());
    if let Some(desc) = &emergency.description {
        template_data.insert("description".to_string(), desc.clone());
    }
    if let Some(city) = &emergency.city {
        template_data.insert("city".to_string(), city.clone());
    }
    if let Some(street) = &emergency.street {
        template_data.insert("street".to_string(), street.clone());
    }

    let event = EmergencyConfirmedEvent {
        emergency_id: emergency.id,
        title: emergency.title.clone(),
        description: emergency.description.clone(),
        city: emergency.city.clone(),
        street: emergency.street.clone(),
        country: emergency.country.clone(),
        template_name: "emergency-alert".to_string(),
        template_data,
    };

    state
        .kafka
        .send(&emergency.id.to_string(), &event)
        .await
        .map_err(AppError::Internal)
}

fn auth_user_id(headers: &HeaderMap, config: &Config) -> Result<uuid::Uuid, AppError> {
    let token = headers
        .get("Authorization")
        .and_then(|v| v.to_str().ok())
        .ok_or_else(|| AppError::Unauthorized("Missing Authorization header".to_string()))?;

    extract_user_id(token, &config.jwt_secret)
}
