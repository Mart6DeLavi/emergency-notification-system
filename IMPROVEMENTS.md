# SENSA — Improvement Log

Chronological engineering log for the SENSA emergency-notification platform.
All dates are in `YYYY-MM-DD` format.

## Change index

| Date | Area | Change | Result | Commit / merge |
|---|---|---|---|---|
| 2026-11-08 | API gateway | Reused one HTTP client and stopped chunking bodyless requests | Gateway TTFB reduced from 0.60–0.85 s to 0.03–0.21 s | `17a45a7` / `ca734a9` |
| 2026-11-07 | user-data-management-service | Added an H2 test profile for the Spring context test | Jenkins Quality no longer needs PostgreSQL/Liquibase | `95d85cd` / `11704d6` |
| 2026-11-04 | Test Lab | Added test-lab-service, test mode and panel run/history UI | Load tests can run without notifications or S3 writes | `48585d4`, `49656b7`, `8a43578`, `a1464bb` / `9cf13ee` |
| 2026-11-03 | Android app | Added native Kotlin/Compose client | Login, reports, upload/moderation, profile work end-to-end | `8ee2ab3` / `7397334` |
| 2026-11-03 | Email | Added responsive HTML templates for five alert types | Fire/accident/weather/evacuation/other emails render correctly | `62b86b7` / `aa9075e` |
| 2026-11-03 | Email delivery | Fixed STARTTLS, RabbitMQ JSON conversion and AWS SDK startup | Brevo email delivery verified end-to-end | `0951693`, `6777b0a`, `4f1dd2e` |
| 2026-11-03 | Rescue panel | Broadcast Overview redesigned and tightened | Operator broadcasts show KPIs, map, donut and delivery chart | `3801a68`, `666274e`, `8960a59`, `d65fe8e` |
| 2026-11-02 | iOS app | Merged the full SwiftUI MVP into master | Auth, reports, alerts, presigned attachments and profile available | `101f7fd` / `bd25217` |
| 2026-11-08 | Operations | Removed stale image tags and dangling Docker images | 1.56 GB reclaimed; all running images and Jenkins history preserved | — |

---

## 2026-11-08 — API gateway latency

### Problem

The rescue panel felt slow even though the business services responded quickly.
Measurements showed that the API gateway added roughly 0.5–1.3 seconds to every request.

### Root cause

1. A new `reqwest::Client` was constructed for every rate-limit call, load-balancer
   lookup and proxied request. That repeated connector/TLS initialization on the hot path.
2. `forward()` always wrapped the Axum request body with `reqwest::Body::wrap_stream`,
   including GET requests with no body. This forced chunked transfer for bodyless requests
   and added avoidable proxy overhead.

### Changes

| File | Change |
|---|---|
| `api-gateway/src/main.rs` | Added one shared `reqwest::Client` to `AppState` with a 90 s idle pool timeout |
| `api-gateway/src/main.rs` | Reused the shared client in rate limiting, load balancing, test-mode lookup and forwarding |
| `api-gateway/src/main.rs` | Only wraps a request body when `content-length > 0` or `transfer-encoding` is present; otherwise uses an empty body |

### Results

| Measurement | Before | After | Improvement |
|---|---:|---:|---:|
| Gateway internal TTFB | 0.602–0.849 s | 0.026–0.210 s | ~4–23× |
| Public gateway via Caddy TTFB | 0.641–1.451 s | 0.155–0.416 s | ~3–5× |
| Direct notification-service TTFB | 0.058–0.111 s | 0.058–0.111 s | baseline |

The remaining public latency is TLS/Caddy/network overhead; the gateway itself is now close
to the direct backend response time.

### Verification

- Branch: `api-gateway-perf`
- Commit: `17a45a7 perf: avoid chunked body for requests without payload`
- Merge: `ca734a9`
- Deployment: `sensa/api-gateway:latest` recreated on the production server
- Jenkins history, logs and build caches were not modified.

### Lesson

Proxy and gateway overhead must be measured before assuming that a database or a business
service is slow. A tiny response (359 bytes) that takes one second is almost never a database
problem; it usually points at the transport or proxy layer.

---

## 2026-11-07 — user-data Quality test profile

### Problem

Jenkins build #61 failed in the `user-data-management-service` Quality stage:

```text
BeanCreationException
  -> UnexpectedLiquibaseException
  -> PSQLException
  -> java.net.ConnectException
```

The Spring context test tried to run Liquibase against a real PostgreSQL instance on
`localhost:5432`, which does not exist inside the Jenkins agent.

### Changes

| File | Change |
|---|---|
| `user-management-service/build.gradle` | Added `com.h2database:h2` and `io.r2dbc:r2dbc-h2` test runtime dependencies |
| `UserDataManagementServiceApplicationTests` | Added `@ActiveProfiles("test")` |
| `src/test/resources/application-test.yaml` | H2 JDBC + R2DBC, Liquibase disabled, Kafka listener auto-startup disabled, Eureka/discovery disabled |

### Result

```text
./gradlew --no-daemon test
BUILD SUCCESSFUL in 17s
```

Commit `95d85cd`, merged as `11704d6`.

---

## 2026-11-04 — Test Lab

### Added

- Rust `test-lab-service` on port `8010`, MongoDB collection `test_runs`.
- Runtime test mode stored in MongoDB and propagated by the gateway through:
  - `X-Sensa-Test-Mode: true`
  - `X-Sensa-Test-Run: <run-id>`
- In test mode:
  - `notification-service` counts recipients but does not persist notifications or send
    email/push/SMS;
  - `filesystem-service` does not touch S3 and returns a fake approved file;
  - test mode is automatically disabled after the run.
- k6 scenarios: `read`, `broadcast`, `mixed`.
- Panel **Test Lab** page:
  - test-mode toggle and banner;
  - scenario/preset/VUs/duration controls;
  - live run polling;
  - total requests, RPS, success rate, p50/p95/p99 and status codes;
  - test history, run detail, stop and delete.

### Verified result

| Metric | Value |
|---|---:|
| Scenario | `read` |
| Virtual users | 5 |
| Duration | 20 s |
| Total requests | 49 |
| RPS | 1.9 |
| Success rate | 100% |
| p50 | 2233 ms |
| p95 / p99 | 3349 ms |
| HTTP 2xx | 49 |

### API endpoints

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/api/v1/test-lab/state` | Public test-mode state |
| `GET/PATCH` | `/api/v1/test-lab/settings` | Read/update test mode |
| `POST/GET` | `/api/v1/test-lab/runs` | Start/list test runs |
| `GET/DELETE` | `/api/v1/test-lab/runs/{id}` | Run detail/delete |
| `POST` | `/api/v1/test-lab/runs/{id}/stop` | Stop a running test |
| `POST` | `/api/v1/test-lab/cleanup` | Clean up test data |

### Known limitation

k6 `--summary-export` does not provide per-endpoint and time-series breakdowns in the current
parser. The `endpoints` and `series` arrays are therefore empty; aggregate metrics are correct.
A later improvement can parse the streaming k6 JSON output for charts and endpoint tables.

---

## 2026-11-03 — Email notifications

### Delivery fixes

| Problem | Fix |
|---|---|
| Port 587 failed with implicit TLS | `starttls_relay()` for port 587, `relay()` for 465 |
| Rust consumer could not parse Spring AMQP messages | Added `Jackson2JsonMessageConverter` and a custom `RabbitTemplate` |
| AWS SDK panicked when SMS was attempted | Added `BehaviorVersion::latest()` to the SNS client |

### HTML templates

Added a `EmailTemplateRenderer` and five responsive Thymeleaf templates:

| Alert type | Template | Visual identity |
|---|---|---|
| `FIRE` | `fire.html` | red header, fire icon |
| `ACCIDENT` | `accident.html` | amber header, car icon |
| `WEATHER` | `weather.html` | blue header, storm icon |
| `EVACUATION` | `evacuation.html` | violet header, evacuation icon |
| `OTHER` | `other.html` | neutral header, warning icon |

The templates use a 600 px card, mobile media queries, escaped HTML, line-break conversion,
affected-area block and panel link. A live Brevo test delivered five emails successfully to
the configured test recipient.

---

## 2026-11-03 — Android client

| Area | Implementation |
|---|---|
| Language/UI | Kotlin 2.0.21, Jetpack Compose, Material 3 |
| Auth | Login/registration with address fields |
| Reports | Rescue-service alerts and personal reports |
| Report detail | Presigned attachment URLs, images and video tile |
| New report | Android Photo Picker, multipart upload, per-file moderation, emergency creation and file linking |
| Profile | Personal data, address, email/push settings |
| Verification | Pixel_9a emulator; 2 reports persisted and visible in My Reports |

Commit `8ee2ab3`, merged as `7397334`.

---

## 2026-11-08 — Server image cleanup

| Item | Action | Result |
|---|---|---|
| Stale `sensa/<service>:<number>` tags | Removed 209 tags; kept `:62` and `latest` | No Jenkins history affected |
| Dangling images | `docker image prune -f` | 1.56 GB reclaimed |
| APT package cache | `apt-get clean` | Reduced package cache |
| Jenkins, logs, databases, build caches | Not touched | Preserved |

Disk after cleanup: **290 GB total, 175 GB used, 116 GB free (61%)**.

---

## Open follow-ups

| Area | Follow-up |
|---|---|
| Test Lab | Parse k6 streaming output for endpoint-level tables and RPS/latency charts |
| Test Lab | Add an `incident` scenario (upload + create emergency + link) |
| Jenkins | Build only changed services; avoid compiling twice (host + Docker) |
| Panel | Consider request batching/caching if pages still issue many sequential calls |
| Mobile | Push notifications remain out of scope; in-app alerts only |
| Documentation | Keep this log updated for each user-visible or performance change |
