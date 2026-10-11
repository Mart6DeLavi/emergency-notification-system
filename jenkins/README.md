# Jenkins — CI/CD для SENSA

Каталог содержит конфигурацию Jenkins для сборки, тестирования, проверки
безопасности и доставки всех микросервисов проекта.

## Структура

- `docker-compose.yml` — Jenkins LTS (controller) + два inbound-агента (`gradle`,
  `rust`), docker-in-docker через проброс `/var/run/docker.sock`.
- `agents/gradle/Dockerfile` — агент для Java/Kotlin: JDK 21 + Docker CLI + git.
- `agents/rust/Dockerfile` — агент для Rust: Docker CLI + `build-essential`,
  `pkg-config`, `libssl-dev`, `librdkafka-dev` + Rust 1.98.
- Корневой `Jenkinsfile` — пайплайн (scripted).
- `services.groovy` — **единый список сервисов** (источник правды для CI).
- `ci.groovy` — тонкий агрегатор, который подключает `vars/*.groovy`.
- `vars/quality.groovy` — этап Quality (тесты).
- `vars/build.groovy` — этап Build (артефакт + Docker-образ).
- `vars/security.groovy` — этап Security (Trivy, блокирующий).
- `vars/deploy.groovy` — этап Deploy (push в registry + опциональный rollout).
- `vars/notify.groovy` — post-хук уведомлений (email заготовлен, но выключен).

## Этапы пайплайна

Для **каждого** из 11 сервисов (все параллельно) выполняются 4 этапа:

1. **Quality** — тесты: Gradle `./gradlew --no-daemon test` / Rust `cargo test`.
   Если тесты падают — остальные этапы для этого сервиса не запускаются.
2. **Build** — сборка артефакта (`assemble` / `cargo build --release`) и
   Docker-образа `sensa/<service>:<BUILD_NUMBER>` + `:latest`.
3. **Security** — блокирующая проверка образа через Trivy
   (`--severity HIGH,CRITICAL --exit-code 1`).
4. **Deploy** — `docker login` + push образа; при заданном `DEPLOY_HOST`
   выполняется `ssh $DEPLOY_HOST 'docker compose pull && docker compose up -d'`.

Сервисы Gradle: authentication-service, discovery-server, filesystem-service,
notification-service, template-service, user-management-service.
Сервисы Rust: emergency-situation-request-service, message-delivery-service,
load-balancer-service, rate-limiter-service, api-gateway.

## Запуск

```bash
# 1. Собрать агентов и поднять Jenkins
cd jenkins
JENKINS_AGENT_GRADLE_SECRET=<secret> JENKINS_AGENT_RUST_SECRET=<secret> docker compose up -d --build

# 2. В Jenkins (Manage Jenkins → Nodes) создать агентов:
#    - gradle (label: gradle)
#    - rust   (label: rust)
#    и указать те же секреты агентов, что и в compose.

# 3. Создать Multibranch/ Pipeline-джобу, указав путь к `Jenkinsfile` в корне репозитория.
```

## Требуемые настройки Jenkins

- **Credential `docker-registry-credentials`** (Username/Password) — для `docker login`.
  Если не задан, Deploy логирует предупреждение, пропускает push и продолжает сборку.
- Переменные (через job parameters / env):
  - `DEPLOY_HOST` — host для выкатки (пусто = только push образа, без rollout).
  - `NOTIFY_ENABLED` — `true` для включения post-хука уведомлений (по умолчанию `false`).
  - `NOTIFY_EMAIL_RECIPIENTS` — получатели (используется, когда `NOTIFY_ENABLED=true`).
- Плагины: Workspace Cleanup (`cleanWs`). Для собственно email — Email Extension
  plugin (`emailext` в `vars/notify.groovy` уже подготовлен, вызов закомментирован).

## Образы агентов

- **gradle**: JDK 21 (из базового образа) + Docker CLI.
- **rust**: Docker CLI + `librdkafka-dev` (для rdkafka с `dynamic-linking`) + Rust 1.98.
- Trivy запускается как отдельный контейнер (`aquasec/trivy:latest`) с кэшем в
  docker-volume `trivy_cache` (создаётся автоматически).

## Замечание по масштабированию

Сервисы одного типа делят один лейбл-агент, поэтому при единственном агенте
соответствующего типа параллельные ветки выполняются последовательно. Чтобы
реально распараллелить, увеличьте число агентов с тем же лейблом.
