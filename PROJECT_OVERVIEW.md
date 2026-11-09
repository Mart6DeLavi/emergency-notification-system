# SENSA — Project Overview

SENSA is a prototype emergency-notification platform inspired by Poland's Alert RCB. It lets people report incidents with photos or videos, lets rescue services verify those reports in a web panel, and broadcasts confirmed alerts to users in an affected area.

The system is built as a microservice platform with a web panel (`rescue-panel`), native Android and iOS clients, a Rust API gateway, and asynchronous messaging for event processing and delivery.

## High-level architecture

```text
Android / iOS / Browser
          |
        Caddy (HTTPS)
          |
     api-gateway:8007
      /      |       \
 rate-limiter  load-balancer  test-lab
      |             |
  business services (HTTP)
      |
  Kafka / RabbitMQ / S3 / PostgreSQL / MongoDB / Redis
```

The Rust gateway owns public routing, rate limiting, instance lookup, and test-mode header injection. Java/Kotlin services register in Eureka; Rust services use the static load-balancer registry.

## Services

| Service | Stack | Port | Storage | Responsibility |
|---|---|---:|---|---|
| `rescue-panel` | Nuxt 4, Vue, TypeScript, PrimeVue | 3000 | — | Operator panel: reports, templates, broadcasts, Test Lab |
| `api-gateway` | Rust, Axum | 8007 | — | Routing, rate-limit check, load balancing, streaming proxy |
| `load-balancer-service` | Rust | 8008 | static YAML | Round-robin instance registry |
| `rate-limiter-service` | Rust | 8009 | Redis | Token-bucket limits per client/service |
| `authentication-service` | Java, Spring Boot | 8001 | PostgreSQL | Register, login, JWT validation |
| `user-management-service` | Java, WebFlux/R2DBC | 8000 | PostgreSQL | Profiles, addresses, notification settings, recipients |
| `template-service` | Kotlin, Spring Boot | 8003 | MongoDB + Redis | Notification templates and cache |
| `notification-service` | Java, Spring Boot | 8004 | MongoDB + Redis | Renders alerts, selects recipients, creates deliveries |
| `message-delivery-service` | Rust, RabbitMQ | 8005 | — | Email, SMS and push delivery workers |
| `filesystem-service` | Kotlin, Spring Boot | 8002 | PostgreSQL + S3 | Upload, ML moderation, file linking, presigned URLs |
| `emergency-situation-request-service` | Rust, Axum | 8006 | PostgreSQL | Incident reports and status workflow |
| `test-lab-service` | Rust, Axum | 8010 | MongoDB | k6 load tests, test mode, run history |
| `discovery-server` | Java, Eureka | 8761 | — | Service discovery for JVM services |

## Communication

**Synchronous HTTP**

- Clients → Caddy → `api-gateway`.
- `notification-service` → `template-service` for template content.
- `notification-service` → `user-management-service` for recipients by city/street.
- `api-gateway` → rate limiter, load balancer, test-lab state.
- `test-lab-service` → `api-gateway` (internal) when running k6.

**Kafka events**

| Topic | Producer | Consumer |
|---|---|---|
| `user.registered` | authentication-service | user-management-service |
| `user.updated`, `user.deleted` | user-management-service | — |
| `emergency.confirmed` | emergency-situation-request-service | notification-service |
| `file.moderated` | filesystem-service | — |

**RabbitMQ**

- Queue `notification.delivery` from notification-service to message-delivery-service.
- Each delivery event carries the channel (`EMAIL`, `PUSH`, `SMS`) and recipient data.

**Other infrastructure**

- **PostgreSQL** — auth, users, files, emergencies.
- **MongoDB** — templates, notifications, operator broadcasts, Test Lab runs.
- **Redis** — template/notification caches and rate limiting.
- **Amazon S3** — uploaded media; access via short-lived presigned URLs.
- **Kafka** — inter-service events; **RabbitMQ** — notification delivery queue.

## Main flows

### 1. Registration

1. Client calls `POST /api/v1/auth/register`.
2. authentication-service stores the BCrypt password and emits `user.registered`.
3. user-management-service consumes the event and creates profile, address and notification settings.

### 2. Incident report

1. User selects photos/videos in the mobile app.
2. App uploads the batch to `filesystem-service`.
3. Each file is moderated by the NSFW ONNX model; result is `APPROVED` or `REJECTED`.
4. Only approved files are submitted with the incident.
5. `POST /api/v1/emergencies` stores the report; `PATCH /api/v1/files/link` links files.
6. Rescue staff see the report in the panel and can move it through `REPORTED → UNDER_REVIEW → CONFIRMED → BROADCAST`.

On `CONFIRMED`, emergency-service emits `emergency.confirmed`.

### 3. Alert broadcast

1. notification-service consumes `emergency.confirmed`.
2. It fetches the template from template-service and renders placeholders.
3. It asks user-management-service for users in the affected city/street.
4. It stores an in-app notification per user and publishes delivery events to RabbitMQ.
5. message-delivery-service sends email (SMTP/Brevo), push (FCM when configured) and SMS (AWS SNS when configured).

### 4. Operator broadcast

1. Operator composes an alert in `/broadcast`.
2. notification-service resolves recipients (all users or city/street).
3. It creates in-app notifications and delivery events; history is stored in MongoDB.

### 5. Test Lab

1. Operator starts a k6 scenario from the panel.
2. test-lab-service enables a runtime test-mode flag and starts k6 against the internal gateway.
3. Gateway injects `X-Sensa-Test-Mode` / `X-Sensa-Test-Run` and bypasses rate limiting.
4. notification-service and filesystem-service suppress persistence and external delivery in test mode.
5. k6 summary + JSON points produce a time series; results and history are stored in MongoDB.

## Security

- JWT HS512, BCrypt password hashing.
- Gateway forwards Authorization headers; protected services validate JWT.
- S3 objects are private; the app requests presigned GET URLs.
- NSFW moderation runs before a file is accepted.
- Test mode is explicitly labelled and isolated from production notifications/uploads.

## Deployment

- Docker Compose runs the stack; Caddy terminates TLS for `panel`, `jenkins`, `grafana` and `k8s` subdomains.
- Jenkins builds 11 services in parallel with Quality → Build → Security → Deploy stages, then performs a Rollout.
- Monitoring: Prometheus, Grafana, Loki/Alloy, cAdvisor, node-exporter.
- Optional k3s/Helm slice exists for a subset of services; Compose remains the primary deployment.

## Current limitations

- Real device push (APNs/FCM) is not enabled; the apps show in-app alerts.
- SMS is wired but AWS SNS permissions/configuration are not enabled; email is configured through Brevo.
- Android/iOS are MVP clients; TestFlight/App Store distribution is not set up.
- Load tests run against the internal gateway; production capacity numbers are project estimates, not a formal benchmark.

## Repository layout

```text
api-gateway/                 Rust edge proxy
authentication-service/      Java auth/JWT
user-management-service/     Java user profiles (service name user-data-management-service)
template-service/            Kotlin templates
notification-service/        Java alert rendering and recipients
message-delivery-service/    Rust delivery workers
filesystem-service/          Kotlin S3 + NSFW moderation
emergency-situation-request-service/  Rust incident workflow
test-lab-service/            Rust load-test control plane
rescue-panel/                Nuxt operator panel
mobile/ios/                  SwiftUI app
mobile/android/              Kotlin/Compose app
docker/                      Caddy, Prometheus, Loki, Grafana, service configs
jenkins/                     CI/CD pipeline and agents
k8s/                         optional k3s/Helm deployment
```
