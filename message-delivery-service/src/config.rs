use std::env;

#[derive(Clone, Debug)]
pub struct Config {
    pub rabbitmq_url: String,
    pub delivery_queue: String,
    pub server_port: u16,
    pub smtp_host: Option<String>,
    pub smtp_port: u16,
    pub smtp_username: Option<String>,
    pub smtp_password: Option<String>,
    pub smtp_from: Option<String>,
    pub smtp_tls: bool,
    pub fcm_api_key: Option<String>,
    pub fcm_project_id: Option<String>,
    pub aws_access_key_id: Option<String>,
    pub aws_secret_access_key: Option<String>,
    pub aws_region: Option<String>,
}

fn opt_env(key: &str) -> Option<String> {
    env::var(key).ok().filter(|v| !v.is_empty())
}

impl Config {
    pub fn from_env() -> Self {
        Self {
            rabbitmq_url: env::var("RABBITMQ_URL")
                .unwrap_or_else(|_| "amqp://localhost:5672/%2f".to_string()),
            delivery_queue: env::var("DELIVERY_QUEUE")
                .unwrap_or_else(|_| "notification.delivery".to_string()),
            server_port: env::var("SERVER_PORT")
                .unwrap_or_else(|_| "8005".to_string())
                .parse()
                .unwrap_or(8005),
            smtp_host: opt_env("SMTP_HOST"),
            smtp_port: env::var("SMTP_PORT")
                .unwrap_or_else(|_| "587".to_string())
                .parse()
                .unwrap_or(587),
            smtp_username: opt_env("SMTP_USERNAME"),
            smtp_password: opt_env("SMTP_PASSWORD"),
            smtp_from: opt_env("SMTP_FROM"),
            smtp_tls: env::var("SMTP_TLS")
                .unwrap_or_else(|_| "true".to_string())
                .parse()
                .unwrap_or(true),
            fcm_api_key: opt_env("FCM_API_KEY"),
            fcm_project_id: opt_env("FCM_PROJECT_ID"),
            aws_access_key_id: opt_env("AWS_ACCESS_KEY_ID"),
            aws_secret_access_key: opt_env("AWS_SECRET_ACCESS_KEY"),
            aws_region: opt_env("AWS_REGION"),
        }
    }
}
