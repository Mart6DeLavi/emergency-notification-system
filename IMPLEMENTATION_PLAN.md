# Полный план проекта SENSA (система экстренных оповещений)

## 1. Обзор системы

Архитектура микросервисная. Всего **9 компонентов**: 7 бизнес-сервисов + 2 инфраструктурных.

**Общий стек:** Spring Boot (Java/Kotlin) + Rust (высоконагруженные) + PostgreSQL + Redis + Kafka + Amazon S3.

**Единые соглашения для всех Java/Kotlin сервисов:**
- JWT (HS256, `jjwt 0.12.6`), claims: `userId` (UUID), `email` (subject), `iat`, `exp` (TTL 24h)
- `JwtAuthFilter` → извлекает `Authorization: Bearer`, кладёт `userId` в `SecurityContext`
- Stateless-сессии, CSRF выключен, `permitAll` только для swagger/health
- Eureka client → `discovery-server:8761`
- Liquibase миграции, `ddl-auto: validate`
- springdoc OpenAPI

---

## 2. Статус по каждому сервису

| # | Сервис | Язык | Статус | Действие |
|---|--------|------|--------|----------|
| 1 | user-data-management-service | Java (WebFlux+R2DBC) | готов | — |
| 2 | authentication-service | Java (Servlet) | готов | — |
| 3 | template-service | Kotlin | готов | — |
| 4 | discovery-server | Java | готов | — |
| 5 | emergency-situation-request-service | Rust | нет | создать с нуля |
| 6 | notification-service | Java | частично | переделать |
| 7 | message-delivery-service | Rust | заглушка (Java `com.coopergroup`) | переписать |
| 8 | filesystem-service | Kotlin | нет | создать с нуля |
| 9 | api-gateway | Java (WebFlux) | старые маршруты | переделать |

---

## 3. Детальный план по сервисам

### 3.1 emergency-situation-request-service — Создать с нуля (Rust)

**Назначение:** история инцидентов, заявки пользователей и подтверждение службами.

**Стек:** `axum + tokio + sqlx (PostgreSQL) + rdkafka (Kafka) + serde/serde_json + jsonwebtoken + uuid + chrono`

**Данные** (`emergencies`):
```
id BIGSERIAL PK · user_id UUID · title · description · files JSONB ·
country · city · street · alarm_timestamp TIMESTAMPTZ ·
status VARCHAR · created_at TIMESTAMPTZ
```

**Статусы:** `REPORTED → UNDER_REVIEW → CONFIRMED → BROADCAST` (+ `REJECTED`)

**REST:**
| Метод | Путь | Описание |
|-------|------|----------|
| POST | `/api/v1/emergencies` | создать заявку (пользователь) |
| GET | `/api/v1/emergencies` | список (панель служб) |
| GET | `/api/v1/emergencies/{id}` | по id |
| PATCH | `/api/v1/emergencies/{id}/status` | смена статуса (служба) |

**Kafka (producer):** при `CONFIRMED` → `emergency.confirmed`
```json
{ "emergencyId": "...", "title": "...", "description": "...",
  "city": "...", "street": "...", "templateName": "...",
  "templateData": { "key": "value", ... } }
```

**Связи:** ← приём файлов (filesystem-service), → Kafka `emergency.confirmed` (notification-service)

---

### 3.2 notification-service — Переделать (Java)

**Назначение:** склеивает шаблон с текстом + определяет получателей (SERVICE_ARCHITECTURE.md).

**Что уже есть:** CRUD уведомлений + Kafka producer. **Чего нет:** рендеринг, выбор получателей, приём `emergency.confirmed`.

**Что добавить:**
1. Kafka **consumer** `emergency.confirmed`
2. **Рендеринг** (как в notification-service2): fetch шаблона из template-service (RestTemplate уже подключён, но не используется), `{{placeholder}}` → value из `templateData`, `\n`→`<br>` для PUSH
3. **Получатели:** запрос к `user-data-management-service` по city+street (нужен новый endpoint — см. 3.3)
4. Для каждого получателя — событие `notification.delivery` в Kafka (по каналу из настроек пользователя)

**Связи:** ← Kafka `emergency.confirmed`, → template-service (рендер), → user-data-management-service (зона), → Kafka `notification.delivery`

---

### 3.3 user-data-management-service — +1 endpoint (Java)

**Что добавить:** `GET /api/v1/users/location?city=&street=` — пользователи в зоне (userId + email + phone + настройки каналов). Реактивный запрос через `UserLocationDataEntity`.

---

### 3.4 message-delivery-service — Переписать (Rust)

**Назначение:** реальная доставка email/push/SMS (THESIS.md: RabbitMQ для уведомлений, но в проекте — Kafka).

**Что сделать:** переписать из Java `com.coopergroup` в Rust `com.sensa` (структура та же что в 3.1):
- Kafka **consumer** `notification.delivery`
- Доставка: email (Amazon SES стаб), push (стаб), SMS (Twilio стаб) — с логированием + статусом

**Связи:** ← Kafka `notification.delivery` (notification-service)

---

### 3.5 filesystem-service — Создать с нуля (Kotlin)

**Назначение:** хранение файлов в S3 + ML-модерация.

**Стек:** Kotlin + Spring Boot + AWS S3 SDK (загрузка через multipart)

**Данные** (`files`):
```
id BIGSERIAL PK · user_id UUID · url · created_at · emergency_situation_id
```

**Что сделать:**
1. `POST /api/v1/files` (multipart) → загрузка в S3 + метаданные
2. Асинхронный `ModerationService` (интерфейс-заглушка, реализацию добавишь позже) → Kafka `file.moderated {fileId, moderateResult}`

**Связи:** → S3, → Kafka `file.moderated` (emergency-situation-request-service)

---

### 3.6 api-gateway — Переделать (Java/WebFlux)

**Что сделать:** финальные маршруты для всех сервисов:
```
/api/v1/auth/**             → authentication-service (8001)
/api/v1/users/**            → user-data-management-service (8000)
/api/v1/templates/**        → template-service (8003)
/api/v1/notifications/**    → notification-service (8004)
/api/v1/emergencies/**      → emergency-situation-request-service (800x)
/api/v1/files/**            → filesystem-service (800x)
```
+ swagger-агрегация всех сервисов.

---

## 4. Схема взаимодействия (поток данных)

```
        ┌────────────────────── Бизнес-поток ──────────────────────┐
Пользователь (app) ──▶ filesystem-service (S3 + ML-модерация)
                              │  Kafka: file.moderated
                              ▼
                 emergency-situation-request-service
                              │  Kafka: emergency.confirmed
                              ▼
                      notification-service ──▶ template-service (рендер)
                              │                   ▲
                              │  HTTP city+street │
                              ▼                   │
                    user-data-management-service ┘
                              │  Kafka: notification.delivery
                              ▼
                    message-delivery-service (email/push/SMS)
```

**Цепочка зависимостей:** `filesystem → emergency → notification → {template, user-data} → message-delivery`

---

## 5. Порядок реализации

| # | Фаза | Действие | Сервис |
|---|------|----------|--------|
| 0 | Setup | `brew install librdkafka` | — |
| 1 | Создать | emergency-situation-request-service | Rust |
| 2 | Дописать | +location endpoint | user-data-management-service |
| 3 | Переделать | рендеринг + получатели + consumer | notification-service |
| 4 | Переписать | доставка | message-delivery-service (Rust) |
| 5 | Создать | S3 + ML-стаб | filesystem-service (Kotlin) |
| 6 | Переделать | маршруты | api-gateway |

---

## 6. Риски / требования

- **Rust:** нужен `brew install librdkafka` (Kafka); sqlx/PostgreSQL — чистый Rust, без нативной либы. Первая компиляция Rust-зависимостей может занять 2-5 мин.
- **S3:** для filesystem-service нужны AWS-креды (или локальный MinIO для прототипа) — уточнить при старте фазы 5.
- **ML:** интерфейс-заглушка, реализация будет добавлена позже.
