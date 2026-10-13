# SENSA — Unified full-stack Docker Compose

Single `docker-compose.yml` that runs the whole system: infrastructure, all
business services (Java/Kotlin/Rust), the custom load-balancer / rate-limiter /
api-gateway (Rust) and the Nuxt rescue-services panel.

## Components

| Service | Image / build | Port (host) |
|---|---|---|
| postgres | `postgres:15.2` (4 DBs via `docker/postgres/init.sql`) | 5432 |
| mongo | `mongo:7` | 27017 |
| redis | `redis:7-alpine` | 6379 |
| zookeeper + kafka | `confluentinc/cp-*:7.3.2` | 2181 / 9092 (+29092 host) |
| rabbitmq | `rabbitmq:3-management` | 5672 / 15672 |
| discovery-server | `./discovery-server` | 8761 |
| authentication-service | `./authentication-service` | internal 8001 |
| user-data-management-service | `./user-management-service` | internal 8000 |
| template-service | `./template-service` | internal 8003 |
| notification-service | `./notification-service` | internal 8004 |
| filesystem-service | `./filesystem-service` | internal 8002 |
| emergency-situation-request-service | `./emergency-situation-request-service` | internal 8006 |
| message-delivery-service | `./message-delivery-service` | internal 8005 |
| rate-limiter-service | `./rate-limiter-service` | internal 8009 |
| load-balancer-service | `./load-balancer-service` | internal 8008 |
| api-gateway | `./api-gateway` | 8080 → 8007 |
| rescue-panel | `./rescue-panel` | 3000 |

## Usage

```bash
cp .env.example .env      # fill in POSTGRES_PASSWORD, JWT_USER_SECRET, AWS_*, S3_BUCKET, ...
docker compose up --build
```

- Panel: http://localhost:3000
- API gateway: http://localhost:8080
- Eureka dashboard: http://localhost:8761
- RabbitMQ management: http://localhost:15672 (user/pass from `.env`)

Stop with `docker compose down` (add `-v` to wipe data volumes).

## Configuration notes

- **Spring services** are configured purely via environment variables in
  `docker-compose.yml` (datasource/Mongo/Redis/RabbitMQ/Kafka/Eureka URLs and
  `JWT_USER_SECRET`). No `application-docker.yaml` is needed.
- **Rust services** (`load-balancer`, `rate-limiter`, `api-gateway`) read a YAML
  file via `CONFIG_PATH`. Container-specific configs live in `docker/` and are
  bind-mounted to `/app/config.yaml`.
- The custom **load-balancer** holds a static registry (`docker/load-balancer/config.yaml`)
  mapping logical service names to container hostnames — this is how the api-gateway
  reaches the Rust/Java/Kotlin services (Eureka is not used for routing).
- Kafka advertises `kafka:9092` internally and `localhost:29092` for host tools.

## Requirements

- Docker with Compose v2.
- Network access during the first build (Gradle/Maven/NPM/Cargo dependencies;
  filesystem-service also downloads a ~327 MB ONNX NSFW model at image build).
