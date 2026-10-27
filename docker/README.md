# SENSA — полный стек через Docker Compose

Единый `docker-compose.yml` (в корне репозитория) поднимает всю систему:
инфраструктуру, бизнес-сервисы, Rust-инфраструктуру
(`api-gateway` / `load-balancer` / `rate-limiter`), веб-панель служб спасения
и стек мониторинга.

Наружу открыты только **80/443** (Caddy + автоматический TLS от Let's Encrypt).
Все остальные порты привязаны к `127.0.0.1` и доступны только с самого сервера.

## Требования

- Docker Engine 24+ и Docker Compose v2
- 8 vCPU / 16–24 GB RAM (сборка образов + Kafka/Mongo/Postgres)
- Исходящий интернет при первой сборке (зависимости + 327 MB ONNX-модель)
- Домен с A-записями `@`, `www`, `panel`, `jenkins`, `grafana` → IP сервера

## Быстрый старт

```bash
cp .env.example .env
# заполнить .env: POSTGRES_PASSWORD, JWT_USER_SECRET, RABBITMQ_*,
# AWS_ACCESS_KEY_ID / AWS_SECRET_ACCESS_KEY / S3_BUCKET,
# GRAFANA_ADMIN_PASSWORD, ACME_EMAIL и домены

docker network create sensa-proxy   # один раз
docker compose up -d --build
```

Если Jenkins ещё не собирал образы, `--build` соберёт их на месте.
В production образы собирает Jenkins (см. `../jenkins/README.md`),
а деплой выполняет `docker compose up -d` с `IMAGE_TAG=<BUILD_NUMBER>`.

## Публичные адреса

| URL | Куда |
|---|---|
| `https://<BASE_DOMAIN>` | редирект на панель |
| `https://<PANEL_DOMAIN>` | веб-панель служб спасения |
| `https://<PANEL_DOMAIN>/api/*` | api-gateway |
| `https://<JENKINS_DOMAIN>` | Jenkins (отдельный compose) |
| `https://<GRAFANA_DOMAIN>` | Grafana |

## Порты

- **Публичные:** `80`, `443` (Caddy).
- **Только localhost (`127.0.0.1`):**
  postgres 5432, mongo 27017, redis 6379, zookeeper 2181,
  kafka 9092/29092, rabbitmq 5672/15672, discovery 8761,
  api-gateway 8080, panel 3000, prometheus 9090, grafana 3001, loki 3100.

> Docker публикует порты в обход `ufw`, поэтому привязка к `127.0.0.1`
> обязательна — она и обеспечивает защиту.

## Сервисы

| Сервис | Внутренний порт | Назначение |
|---|---|---|
| discovery-server | 8761 | Eureka |
| authentication-service | 8001 | регистрация/логин, JWT |
| user-data-management-service | 8000 | профили, адреса, настройки уведомлений |
| template-service | 8003 | шаблоны (MongoDB) |
| notification-service | 8004 | рендер + выбор получателей |
| filesystem-service | 8002 | S3 + NSFW-модерация |
| emergency-situation-request-service | 8006 | происшествия, статусы |
| message-delivery-service | 8005 | email / push / SMS |
| rate-limiter-service | 8009 | token bucket (Redis) |
| load-balancer-service | 8008 | round-robin |
| api-gateway | 8007 | единая точка входа |
| rescue-panel | 3000 | Nuxt-панель служб спасения |
| caddy | 80 / 443 | TLS + reverse proxy |

## Мониторинг

| Компонент | Адрес | Назначение |
|---|---|---|
| Prometheus | `127.0.0.1:9090` | сбор метрик |
| Grafana | `https://<GRAFANA_DOMAIN>` (внутри `127.0.0.1:3001`) | дашборды |
| Loki | `127.0.0.1:3100` | хранение логов |
| Alloy | — | сбор логов Docker → Loki |
| node-exporter | — | метрики хоста |
| cAdvisor | — | метрики контейнеров |

Дашборды автоматически провижнятся из `grafana/dashboards/`
(Node Exporter Full, containers, services, logs).

Приложение отдаёт метрики:
- Java/Kotlin — `/actuator/prometheus` (actuator + micrometer);
- Rust — `/metrics` (axum-prometheus).

## Конфигурация

- Java/Kotlin-сервисы читают только переменные окружения
  (`SPRING_*`, `JWT_USER_SECRET`, `AWS_*`, …).
- `load-balancer-service`, `rate-limiter-service`, `api-gateway` читают
  `CONFIG_PATH` (файлы в `docker/*/config.yaml`); внутри — только имена контейнеров.
- `IMAGE_TAG` выбирает тег образов (`latest` или номер сборки Jenkins).
- `DOCKER_IMAGE_PREFIX` — префикс образов (по умолчанию `sensa`).

## Обновление

```bash
git pull
docker compose up -d --build
```

В production обновление выполняет Jenkins (стадия **Rollout**).
