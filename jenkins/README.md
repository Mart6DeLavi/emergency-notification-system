# Jenkins — CI/CD для SENSA

Каталог содержит конфигурацию Jenkins для сборки, тестирования и доставки всех микросервисов проекта.

## Структура

- `docker-compose.yml` — Jenkins LTS (controller) + два inbound-агента (`gradle`, `rust`), docker-in-docker через проброс `/var/run/docker.sock`.
- Корневой `Jenkinsfile` — декларативный пайплайн.

## Запуск

```bash
# 1. Поднять Jenkins с агентами
cd jenkins
JENKINS_AGENT_GRADLE_SECRET=<secret> JENKINS_AGENT_RUST_SECRET=<secret> docker compose up -d

# 2. Настроить агентов в Jenkins (Manage Jenkins → Nodes):
#    - gradle (label: gradle)
#    - rust   (label: rust)

# 3. Создать pipeline-джоб, указав путь к Jenkinsfile в корне репозитория.
```

## Стадии пайплайна (корневой `Jenkinsfile`)

1. **Build & Test Java/Kotlin** — `./gradlew --no-daemon clean build` для 6 сервисов (параллельно):
   authentication-service, discovery-server, filesystem-service, notification-service,
   template-service, user-management-service.
2. **Build & Test Rust** — `cargo build && cargo test` для 5 сервисов (параллельно):
   emergency-situation-request-service, message-delivery-service, load-balancer-service,
   rate-limiter-service, api-gateway.
3. **Docker Build & Push** — сборка и публикация образов (`sensa/<service>:<build>` + `:latest`).

## Требования к агентам

- **gradle**: JDK 21 + Gradle (используется wrapper `./gradlew`).
- **rust**: rustc 1.98+ + cargo + `librdkafka-dev` (для rdkafka с `dynamic-linking`).
- **docker**: доступ к Docker daemon (`/var/run/docker.sock`).
