use crate::error::AppError;
use crate::model::Emergency;
use sqlx::PgPool;
use uuid::Uuid;

#[derive(Clone)]
pub struct EmergencyRepository {
    pool: PgPool,
}

impl EmergencyRepository {
    pub fn new(pool: PgPool) -> Self {
        Self { pool }
    }

    pub async fn create(
        &self,
        user_id: Uuid,
        title: &str,
        description: Option<&str>,
        files: Option<serde_json::Value>,
        country: Option<&str>,
        city: Option<&str>,
        street: Option<&str>,
        alarm_timestamp: Option<chrono::DateTime<chrono::Utc>>,
    ) -> Result<Emergency, AppError> {
        let row = sqlx::query_as::<_, Emergency>(
            r#"
            INSERT INTO emergencies
                (user_id, title, description, files, country, city, street, alarm_timestamp, status)
            VALUES ($1, $2, $3, $4, $5, $6, $7, $8, 'REPORTED')
            RETURNING id, user_id, title, description, files, country, city, street,
                      alarm_timestamp, status, created_at
            "#,
        )
        .bind(user_id)
        .bind(title)
        .bind(description)
        .bind(files)
        .bind(country)
        .bind(city)
        .bind(street)
        .bind(alarm_timestamp)
        .fetch_one(&self.pool)
        .await?;

        Ok(row)
    }

    pub async fn find_all(&self) -> Result<Vec<Emergency>, AppError> {
        let rows = sqlx::query_as::<_, Emergency>(
            r#"
            SELECT id, user_id, title, description, files, country, city, street,
                   alarm_timestamp, status, created_at
            FROM emergencies
            ORDER BY created_at DESC
            "#,
        )
        .fetch_all(&self.pool)
        .await?;

        Ok(rows)
    }

    pub async fn find_by_id(&self, id: i64) -> Result<Option<Emergency>, AppError> {
        let row = sqlx::query_as::<_, Emergency>(
            r#"
            SELECT id, user_id, title, description, files, country, city, street,
                   alarm_timestamp, status, created_at
            FROM emergencies
            WHERE id = $1
            "#,
        )
        .bind(id)
        .fetch_optional(&self.pool)
        .await?;

        Ok(row)
    }

    pub async fn update_status(&self, id: i64, status: &str) -> Result<Option<Emergency>, AppError> {
        let row = sqlx::query_as::<_, Emergency>(
            r#"
            UPDATE emergencies
            SET status = $2
            WHERE id = $1
            RETURNING id, user_id, title, description, files, country, city, street,
                      alarm_timestamp, status, created_at
            "#,
        )
        .bind(id)
        .bind(status)
        .fetch_optional(&self.pool)
        .await?;

        Ok(row)
    }
}
