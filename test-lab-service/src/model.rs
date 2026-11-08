use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TestModeState {
    #[serde(rename = "_id")]
    pub id: String,
    pub enabled: bool,
    pub run_id: Option<String>,
    pub expires_at: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TestRun {
    #[serde(rename = "_id")]
    pub id: String,
    pub scenario: String,
    pub status: String,
    pub vus: u32,
    pub duration_sec: u64,
    pub ramp_up_sec: u64,
    pub target: String,
    pub started_at: String,
    pub finished_at: Option<String>,
    pub total_requests: i64,
    pub rps: f64,
    pub success_rate: f64,
    pub count_2xx: i64,
    pub count_3xx: i64,
    pub count_4xx: i64,
    pub count_5xx: i64,
    pub p50: f64,
    pub p95: f64,
    pub p99: f64,
    pub endpoints: Vec<EndpointResult>,
    pub series: Vec<SeriesPoint>,
    pub test_mode: bool,
    pub operator_id: Option<String>,
    pub pid: Option<u32>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct EndpointResult {
    pub path: String,
    pub requests: i64,
    pub errors: i64,
    pub p95: f64,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SeriesPoint {
    pub t: i64,
    pub rps: f64,
    #[serde(default)]
    pub p95: f64,
    #[serde(default)]
    pub vus: f64,
    #[serde(default)]
    pub success: f64,
}

#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct StartRunRequest {
    pub scenario: Option<String>,
    pub vus: Option<u32>,
    pub duration_sec: Option<u64>,
    pub ramp_up_sec: Option<u64>,
}

#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SettingsRequest {
    pub enabled: bool,
    pub ttl_seconds: Option<u64>,
    pub run_id: Option<String>,
}

#[derive(Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct RunListResponse {
    pub items: Vec<TestRun>,
    pub total: u64,
}

#[derive(Debug, Serialize)]
pub struct CleanupResponse {
    pub deleted: u64,
}
