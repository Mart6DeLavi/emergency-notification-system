use axum::extract::{Path, Query, State};
use axum::http::{HeaderMap, StatusCode};
use axum::response::{IntoResponse, Response};
use axum::routing::{delete, get, patch, post};
use axum::{Json, Router};
use chrono::{Duration, Utc};
use futures_util::TryStreamExt;
use mongodb::bson::{doc, Bson};
use mongodb::{Client, Collection};
use serde::Deserialize;
use serde_json::{json, Value};
use std::collections::BTreeMap;
use std::fs::File;
use std::io::{BufRead, BufReader};
use std::process::Stdio;
use std::sync::Arc;

mod auth;
mod config;
mod model;

use auth::extract_user_id;
use config::Config;
use model::{
    CleanupResponse, RunListResponse, SettingsRequest, StartRunRequest, TestModeState, TestRun,
};

#[derive(Clone)]
struct AppState {
    config: Config,
    runs: Collection<TestRun>,
    settings: Collection<TestModeState>,
}

#[derive(Debug)]
enum AppError {
    Unauthorized(String),
    BadRequest(String),
    NotFound(String),
    Internal(String),
}

impl IntoResponse for AppError {
    fn into_response(self) -> Response {
        let (status, message) = match self {
            AppError::Unauthorized(m) => (StatusCode::UNAUTHORIZED, m),
            AppError::BadRequest(m) => (StatusCode::BAD_REQUEST, m),
            AppError::NotFound(m) => (StatusCode::NOT_FOUND, m),
            AppError::Internal(m) => (StatusCode::INTERNAL_SERVER_ERROR, m),
        };
        (status, Json(json!({ "error": message }))).into_response()
    }
}

impl From<mongodb::error::Error> for AppError {
    fn from(e: mongodb::error::Error) -> Self {
        AppError::Internal(e.to_string())
    }
}

#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
struct RunQuery {
    scenario: Option<String>,
    status: Option<String>,
    limit: Option<u32>,
    offset: Option<u32>,
}

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    tracing_subscriber::fmt()
        .with_env_filter(
            tracing_subscriber::EnvFilter::try_from_default_env()
                .unwrap_or_else(|_| "test_lab_service=info".into()),
        )
        .init();

    let config = Config::from_env();
    let client = Client::with_uri_str(&config.mongo_uri).await?;
    let db = client.database("Sensa_Test_Lab_Service");
    let runs: Collection<TestRun> = db.collection("test_runs");
    let settings: Collection<TestModeState> = db.collection("test_mode");

    if settings.find_one(doc! { "_id": "state" }).await?.is_none() {
        let initial = TestModeState {
            id: "state".to_string(),
            enabled: false,
            run_id: None,
            expires_at: None,
        };
        let _ = settings.insert_one(&initial).await;
    }

    let state = AppState {
        config: config.clone(),
        runs,
        settings,
    };

    let app = Router::new()
        .route("/actuator/health", get(health))
        .route("/api/v1/test-lab/state", get(get_state))
        .route("/api/v1/test-lab/settings", get(get_settings).patch(patch_settings))
        .route("/api/v1/test-lab/runs", post(start_run).get(list_runs))
        .route("/api/v1/test-lab/runs/{id}", get(get_run).delete(delete_run))
        .route("/api/v1/test-lab/runs/{id}/stop", post(stop_run))
        .route("/api/v1/test-lab/cleanup", post(cleanup))
        .with_state(state);

    let addr = std::net::SocketAddr::from(([0, 0, 0, 0], config.server_port));
    tracing::info!("test-lab-service listening on {}", addr);
    let listener = tokio::net::TcpListener::bind(addr).await?;
    axum::serve(listener, app).await?;
    Ok(())
}

async fn health() -> Json<Value> {
    Json(json!({ "status": "UP" }))
}

fn require_user(headers: &HeaderMap, state: &AppState) -> Result<String, AppError> {
    extract_user_id(headers, &state.config.jwt_secret).map_err(AppError::Unauthorized)
}

async fn active_state(state: &AppState) -> Result<TestModeState, AppError> {
    let mut current = state
        .settings
        .find_one(doc! { "_id": "state" })
        .await?
        .unwrap_or(TestModeState {
            id: "state".to_string(),
            enabled: false,
            run_id: None,
            expires_at: None,
        });

    if current.enabled {
        if let Some(expires) = current.expires_at.as_deref() {
            let expired = chrono::DateTime::parse_from_rfc3339(expires)
                .map(|dt| dt.with_timezone(&Utc) < Utc::now())
                .unwrap_or(false);
            if expired {
                current.enabled = false;
                current.run_id = None;
                current.expires_at = None;
                let _ = state
                    .settings
                    .update_one(
                        doc! { "_id": "state" },
                        doc! { "$set": { "enabled": false, "runId": null, "expiresAt": null } },
                    )
                    .await;
            }
        }
    }
    Ok(current)
}

async fn get_state(State(state): State<AppState>) -> Result<Json<TestModeState>, AppError> {
    Ok(Json(active_state(&state).await?))
}

async fn get_settings(
    State(state): State<AppState>,
    headers: HeaderMap,
) -> Result<Json<TestModeState>, AppError> {
    let _ = require_user(&headers, &state)?;
    Ok(Json(active_state(&state).await?))
}

async fn patch_settings(
    State(state): State<AppState>,
    headers: HeaderMap,
    Json(request): Json<SettingsRequest>,
) -> Result<Json<TestModeState>, AppError> {
    let _ = require_user(&headers, &state)?;
    let run_id = if request.enabled {
        Some(
            request
                .run_id
                .unwrap_or_else(|| uuid::Uuid::new_v4().to_string()),
        )
    } else {
        None
    };
    let expires_at = if request.enabled {
        Some(
            (Utc::now() + Duration::seconds(request.ttl_seconds.unwrap_or(1800) as i64))
                .to_rfc3339(),
        )
    } else {
        None
    };

    state
        .settings
        .update_one(
            doc! { "_id": "state" },
            doc! {
                "$set": {
                    "enabled": request.enabled,
                    "runId": run_id.clone(),
                    "expiresAt": expires_at.clone()
                }
            },
        )
        .upsert(true)
        .await?;

    Ok(Json(TestModeState {
        id: "state".to_string(),
        enabled: request.enabled,
        run_id,
        expires_at,
    }))
}

async fn start_run(
    State(state): State<AppState>,
    headers: HeaderMap,
    Json(request): Json<StartRunRequest>,
) -> Result<Json<TestRun>, AppError> {
    let operator_id = require_user(&headers, &state)?;
    let scenario = request.scenario.unwrap_or_else(|| "read".to_string());
    if !matches!(scenario.as_str(), "read" | "broadcast" | "mixed") {
        return Err(AppError::BadRequest(format!(
            "Unsupported scenario: {}",
            scenario
        )));
    }
    let vus = request.vus.unwrap_or(10).clamp(1, 150);
    let duration_sec = request.duration_sec.unwrap_or(60).clamp(10, 600);
    let ramp_up_sec = request.ramp_up_sec.unwrap_or(0).clamp(0, 300);
    let run_id = uuid::Uuid::new_v4().to_string();
    let started_at = Utc::now().to_rfc3339();

    let run = TestRun {
        id: run_id.clone(),
        scenario: scenario.clone(),
        status: "RUNNING".to_string(),
        vus,
        duration_sec,
        ramp_up_sec,
        target: state.config.api_base_url.clone(),
        started_at: started_at.clone(),
        finished_at: None,
        total_requests: 0,
        rps: 0.0,
        success_rate: 0.0,
        count_2xx: 0,
        count_3xx: 0,
        count_4xx: 0,
        count_5xx: 0,
        p50: 0.0,
        p95: 0.0,
        p99: 0.0,
        endpoints: Vec::new(),
        series: Vec::new(),
        test_mode: true,
        operator_id: Some(operator_id),
        pid: None,
    };

    state.runs.insert_one(&run).await?;
    state
        .settings
        .update_one(
            doc! { "_id": "state" },
            doc! {
                "$set": {
                    "enabled": true,
                    "runId": run_id.clone(),
                    "expiresAt": (Utc::now() + Duration::seconds(duration_sec as i64 + 300)).to_rfc3339()
                }
            },
        )
        .upsert(true)
        .await?;

    let state_for_task = state.clone();
    let run_id_for_task = run_id.clone();
    let scenario_for_task = scenario.clone();
    tokio::spawn(async move {
        run_k6(
            state_for_task,
            run_id_for_task,
            scenario_for_task,
            vus,
            duration_sec,
        )
        .await;
    });

    Ok(Json(run))
}

async fn run_k6(
    state: AppState,
    run_id: String,
    scenario: String,
    vus: u32,
    duration_sec: u64,
) {
    let summary_path = format!("/tmp/k6-{}.json", run_id);
    let points_path = format!("/tmp/k6-{}.jsonl", run_id);
    let duration = format!("{}s", duration_sec);

    let child = tokio::process::Command::new(&state.config.k6_bin)
        .arg("run")
        .arg("--quiet")
        .arg("--out")
        .arg(format!("json={}", points_path))
        .arg("--summary-export")
        .arg(&summary_path)
        .arg(&state.config.k6_script)
        .env("BASE_URL", &state.config.api_base_url)
        .env("SCENARIO", &scenario)
        .env("VUS", vus.to_string())
        .env("DURATION", &duration)
        .env("TEST_RUN_ID", &run_id)
        .env("TEST_EMAIL", &state.config.test_email)
        .env("TEST_PASSWORD", &state.config.test_password)
        .stdout(Stdio::piped())
        .stderr(Stdio::piped())
        .spawn();

    let mut child = match child {
        Ok(child) => child,
        Err(e) => {
            mark_failed(&state, &run_id, &format!("k6 failed to start: {}", e)).await;
            return;
        }
    };

    if let Some(pid) = child.id() {
        let _ = state
            .runs
            .update_one(
                doc! { "_id": &run_id },
                doc! { "$set": { "pid": pid as i32 } },
            )
            .await;
    }

    let output = child.wait_with_output().await;
    let status = output.as_ref().map(|o| o.status.success()).unwrap_or(false);
    let stderr = output
        .as_ref()
        .map(|o| String::from_utf8_lossy(&o.stderr).to_string())
        .unwrap_or_default();

    if status {
        let series = parse_series(&points_path);
        match std::fs::read_to_string(&summary_path) {
            Ok(contents) => match serde_json::from_str::<Value>(&contents) {
                Ok(summary) => mark_completed(&state, &run_id, &summary, &series).await,
                Err(e) => mark_failed(&state, &run_id, &format!("Invalid k6 summary: {}", e)).await,
            },
            Err(e) => mark_failed(&state, &run_id, &format!("Missing k6 summary: {}", e)).await,
        }
    } else {
        mark_failed(&state, &run_id, &format!("k6 exited with error: {}", stderr)).await;
    }

    let _ = std::fs::remove_file(&summary_path);
    let _ = std::fs::remove_file(&points_path);
    disable_if_run(&state, &run_id).await;
}

#[derive(Default)]
struct SeriesBucket {
    requests: i64,
    failures: i64,
    vus: f64,
    durations: Vec<f64>,
}

fn parse_series(path: &str) -> Vec<model::SeriesPoint> {
    let file = match File::open(path) {
        Ok(file) => file,
        Err(_) => return Vec::new(),
    };

    let mut buckets: BTreeMap<i64, SeriesBucket> = BTreeMap::new();
    let mut base: Option<chrono::DateTime<Utc>> = None;

    for line in BufReader::new(file).lines().map_while(Result::ok) {
        let Ok(point) = serde_json::from_str::<Value>(&line) else {
            continue;
        };
        if point.get("type").and_then(Value::as_str) != Some("Point") {
            continue;
        }
        let Some(metric) = point.get("metric").and_then(Value::as_str) else {
            continue;
        };
        let Some(data) = point.get("data") else {
            continue;
        };
        let Some(value) = data.get("value").and_then(Value::as_f64) else {
            continue;
        };
        let Some(time) = data
            .get("time")
            .and_then(Value::as_str)
            .and_then(|value| chrono::DateTime::parse_from_rfc3339(value).ok())
            .map(|value| value.with_timezone(&Utc))
        else {
            continue;
        };

        let base_time = *base.get_or_insert(time);
        let offset = (time - base_time).num_seconds().max(0);
        let bucket = buckets.entry(offset).or_default();

        match metric {
            "http_reqs" => bucket.requests += 1,
            "http_req_failed" => {
                if value >= 1.0 {
                    bucket.failures += 1;
                }
            }
            "http_req_duration" => bucket.durations.push(value),
            "vus" => bucket.vus = value,
            _ => {}
        }
    }

    buckets
        .into_iter()
        .map(|(t, mut bucket)| {
            bucket.durations.sort_by(|a, b| a.partial_cmp(b).unwrap_or(std::cmp::Ordering::Equal));
            let count = bucket.durations.len();
            let p95 = if count == 0 {
                0.0
            } else {
                let index = ((count as f64 * 0.95).ceil() as usize).saturating_sub(1);
                bucket.durations[index.min(count - 1)]
            };
            let success = if bucket.requests == 0 {
                100.0
            } else {
                (bucket.requests - bucket.failures).max(0) as f64 / bucket.requests as f64 * 100.0
            };

            model::SeriesPoint {
                t,
                rps: bucket.requests as f64,
                p95,
                vus: bucket.vus,
                success,
            }
        })
        .collect()
}

fn f64_at(value: &Value, keys: &[&str]) -> f64 {
    let mut current = value;
    for key in keys {
        current = current.get(*key).unwrap_or(&Value::Null);
    }
    current.as_f64().unwrap_or(0.0)
}

/// k6 v0.54 exports metrics at `metrics.<name>.<key>`, while older
/// versions nested them under `metrics.<name>.values.<key>`. Read both.
fn metric_f64(summary: &Value, metric: &str, key: &str) -> f64 {
    let direct = f64_at(summary, &["metrics", metric, key]);
    if direct != 0.0 {
        return direct;
    }
    f64_at(summary, &["metrics", metric, "values", key])
}

async fn mark_completed(
    state: &AppState,
    run_id: &str,
    summary: &Value,
    series: &[model::SeriesPoint],
) {
    let count = metric_f64(summary, "http_reqs", "count");
    let rate = metric_f64(summary, "http_reqs", "rate");
    let failed = metric_f64(summary, "http_req_failed", "value");
    let p50 = metric_f64(summary, "http_req_duration", "med");
    let p95 = metric_f64(summary, "http_req_duration", "p(95)");
    let p99 = {
        let value = metric_f64(summary, "http_req_duration", "p(99)");
        if value > 0.0 { value } else { p95 }
    };
    let total = count.round() as i64;
    let failures = (count * failed).round() as i64;
    let series_bson = mongodb::bson::to_bson(&series).unwrap_or_else(|_| Bson::Array(vec![]));

    let update = doc! {
        "$set": {
            "status": "COMPLETED",
            "finishedAt": Utc::now().to_rfc3339(),
            "totalRequests": total,
            "rps": rate,
            "successRate": (1.0 - failed) * 100.0,
            "count2xx": (total - failures).max(0),
            "count3xx": 0_i64,
            "count4xx": failures,
            "count5xx": 0_i64,
            "p50": p50,
            "p95": p95,
            "p99": p99,
            "series": series_bson,
            "pid": null,
        }
    };
    let _ = state.runs.update_one(doc! { "_id": run_id }, update).await;
}

async fn mark_failed(state: &AppState, run_id: &str, message: &str) {
    tracing::error!("Test run {} failed: {}", run_id, message);
    let _ = state
        .runs
        .update_one(
            doc! { "_id": run_id },
            doc! {
                "$set": {
                    "status": "FAILED",
                    "finishedAt": Utc::now().to_rfc3339(),
                    "pid": null,
                }
            },
        )
        .await;
}

async fn disable_if_run(state: &AppState, run_id: &str) {
    if let Ok(Some(current)) = state.settings.find_one(doc! { "_id": "state" }).await {
        if current.run_id.as_deref() == Some(run_id) {
            let _ = state
                .settings
                .update_one(
                    doc! { "_id": "state" },
                    doc! { "$set": { "enabled": false, "runId": null, "expiresAt": null } },
                )
                .await;
        }
    }
}

async fn list_runs(
    State(state): State<AppState>,
    headers: HeaderMap,
    Query(query): Query<RunQuery>,
) -> Result<Json<RunListResponse>, AppError> {
    let _ = require_user(&headers, &state)?;
    let mut filter = doc! {};
    if let Some(scenario) = query.scenario.filter(|v| !v.is_empty()) {
        filter.insert("scenario", scenario);
    }
    if let Some(status) = query.status.filter(|v| !v.is_empty()) {
        filter.insert("status", status);
    }

    let total = state.runs.count_documents(filter.clone()).await?;
    let limit = query.limit.unwrap_or(20).clamp(1, 100) as i64;
    let offset = query.offset.unwrap_or(0) as i64;

    let items: Vec<TestRun> = state
        .runs
        .find(filter)
        .sort(doc! { "startedAt": -1 })
        .skip(offset as u64)
        .limit(limit)
        .await?
        .try_collect()
        .await?;

    Ok(Json(RunListResponse { items, total }))
}

async fn get_run(
    State(state): State<AppState>,
    headers: HeaderMap,
    Path(id): Path<String>,
) -> Result<Json<TestRun>, AppError> {
    let _ = require_user(&headers, &state)?;
    state
        .runs
        .find_one(doc! { "_id": &id })
        .await?
        .map(Json)
        .ok_or_else(|| AppError::NotFound(format!("Run {} not found", id)))
}

async fn stop_run(
    State(state): State<AppState>,
    headers: HeaderMap,
    Path(id): Path<String>,
) -> Result<Json<TestRun>, AppError> {
    let _ = require_user(&headers, &state)?;
    let run = state
        .runs
        .find_one(doc! { "_id": &id })
        .await?
        .ok_or_else(|| AppError::NotFound(format!("Run {} not found", id)))?;

    if let Some(pid) = run.pid {
        let _ = tokio::process::Command::new("kill")
            .arg("-TERM")
            .arg(pid.to_string())
            .status()
            .await;
    }
    state
        .runs
        .update_one(
            doc! { "_id": &id },
            doc! {
                "$set": {
                    "status": "STOPPED",
                    "finishedAt": Utc::now().to_rfc3339(),
                    "pid": null
                }
            },
        )
        .await?;
    disable_if_run(&state, &id).await;

    state
        .runs
        .find_one(doc! { "_id": &id })
        .await?
        .map(Json)
        .ok_or_else(|| AppError::NotFound(format!("Run {} not found", id)))
}

async fn delete_run(
    State(state): State<AppState>,
    headers: HeaderMap,
    Path(id): Path<String>,
) -> Result<Json<Value>, AppError> {
    let _ = require_user(&headers, &state)?;
    let result = state.runs.delete_one(doc! { "_id": &id }).await?;
    Ok(Json(json!({ "deleted": result.deleted_count })))
}

async fn cleanup(
    State(state): State<AppState>,
    headers: HeaderMap,
) -> Result<Json<CleanupResponse>, AppError> {
    let _ = require_user(&headers, &state)?;
    let result = state
        .runs
        .delete_many(doc! { "status": { "$ne": "RUNNING" } })
        .await?;
    let _ = state
        .settings
        .update_one(
            doc! { "_id": "state" },
            doc! { "$set": { "enabled": false, "runId": null, "expiresAt": null } },
        )
        .await;
    Ok(Json(CleanupResponse {
        deleted: result.deleted_count,
    }))
}
