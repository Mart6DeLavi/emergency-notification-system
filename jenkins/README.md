# Jenkins — CI/CD для SENSA

Пайплайн собирает все 11 сервисов, прогоняет тесты, сканирует образы на
уязвимости и выполняет деплой на сервер. Конфигурация рассчитана на production:
Jenkins живёт на том же хосте, что и стек, и выкатывает его по SSH.

## Структура

| Файл | Назначение |
|------|------------|
| `docker-compose.yml` | Jenkins LTS controller + inbound-агенты `gradle` и `rust`; монтирует `docker.sock`, ходит в сеть `sensa-proxy` |
| `agents/gradle/Dockerfile` | JDK 21 + Docker CLI + git + `openssh-client` |
| `agents/rust/Dockerfile` | Docker CLI + `build-essential`, `pkg-config`, `libssl-dev`, `librdkafka-dev`, Rust 1.98 + `openssh-client` |
| `../Jenkinsfile` | Scripted pipeline: параллельно по сервисам, 4 стадии + Rollout |
| `services.groovy` | Единственный источник списка сервисов и меток агентов |
| `ci.groovy` | Тонкий агрегатор стадий |
| `vars/quality.groovy` | Стадия Quality (тесты) |
| `vars/build.groovy` | Стадия Build (артефакт + Docker-образ) |
| `vars/security.groovy` | Стадия Security (Trivy) |
| `vars/deploy.groovy` | Стадия Deploy (push) + `rolloutAll()` (выкатка) |
| `vars/notify.groovy` | Уведомления (email подготовлен, по умолчанию выключен) |

## Этапы пайплайна

Для каждого из 11 сервисов параллельно выполняются 4 стадии:

1. **Quality** — тесты: `./gradlew --no-daemon test` (Gradle) или `cargo test` (Rust).
2. **Build** — артефакт и Docker-образ `sensa/<service>:<BUILD_NUMBER>` + `:latest`.
   Образы создаются на Docker-демоне хоста (агенты монтируют `docker.sock`),
   поэтому реестр образов не обязателен.
3. **Security** — блокирующее сканирование Trivy (`HIGH,CRITICAL`,
   `--exit-code 1`). Кэш БД Trivy живёт в named volume `trivy_cache`.
4. **Deploy** — опциональный push в реестр (`docker-registry-credentials`).
   Если credential не настроен, push пропускается с предупреждением.

После того как все сервисы прошли свои стадии, выполняется одна общая стадия:

5. **Rollout** — `ci.rolloutAll()` по SSH на хост:
   ```bash
   cd /opt/sensa \
     && git fetch origin master \
     && git reset --hard origin/master \
     && IMAGE_TAG=<BUILD_NUMBER> docker compose up -d \
     && IMAGE_TAG=<BUILD_NUMBER> docker compose up -d --no-deps --build rescue-panel
   ```
   Бэкенд-образы уже лежат на Docker-демоне хоста после стадии Build,
   поэтому пересобирается только `rescue-panel` (её Jenkins не собирает).

Списки сервисов:

- **Gradle (6):** `authentication-service`, `discovery-server`, `filesystem-service`,
  `notification-service`, `template-service`, `user-management-service`.
- **Rust (5):** `emergency-situation-request-service`, `message-delivery-service`,
  `load-balancer-service`, `rate-limiter-service`, `api-gateway`.

## Запуск (на сервере)

```bash
cd /opt/sensa/jenkins
cp .env.example .env          # заполнить секреты агентов (см. ниже)
docker compose up -d --build
```

1. Открой `https://jenkins.<BASE_DOMAIN>` после того, как Caddy получит сертификат.
2. Заверши setup wizard (пароль: `docker exec sensa-jenkins cat /var/jenkins_home/secrets/initialAdminPassword`).
3. Установи плагины: **Pipeline**, **Git**, **Credentials Binding**,
   **Email Extension** (для будущих уведомлений), **Workspace Cleanup**.
4. Создай ноды:
   - `gradle` — label `gradle`, executors 3;
   - `rust` — label `rust`, executors 2.
   Способ подключения: «Launch agent by connecting it to the controller».
   Полученные секреты положи в `jenkins/.env`:
   ```
   JENKINS_AGENT_GRADLE_SECRET=...
   JENKINS_AGENT_RUST_SECRET=...
   ```
   и перезапусти агентов: `docker compose up -d jenkins-agent-gradle jenkins-agent-rust`.
5. Создай Pipeline job:
   - SCM: публичный репозиторий проекта;
   - Branch: `master`;
   - Script Path: `Jenkinsfile`;
   - опционально webhook на `https://jenkins.<BASE_DOMAIN>/github-webhook/`
     (или Poll SCM).

## Требуемые настройки Jenkins

### Credentials

| ID | Тип | Назначение |
|----|-----|------------|
| `sensa-deploy-ssh` | SSH Username with private key, username `deploy` | Rollout по SSH на хост |
| `docker-registry-credentials` | Username/Password | Опционально: push образов в реестр |

Ключ `sensa-deploy-ssh` создаётся на сервере:

```bash
sudo -u deploy ssh-keygen -t ed25519 -N '' -f /home/deploy/.ssh/jenkins_deploy -C jenkins-deploy
sudo -u deploy sh -c 'cat /home/deploy/.ssh/jenkins_deploy.pub >> /home/deploy/.ssh/authorized_keys'
sudo cat /home/deploy/.ssh/jenkins_deploy      # вставить в Jenkins credential
```

### Переменные окружения (Jenkinsfile / job)

| Переменная | По умолчанию | Назначение |
|------------|--------------|------------|
| `DEPLOY_HOST` | `host.docker.internal` | SSH-хост для Rollout |
| `DEPLOY_PATH` | `/opt/sensa` | Путь к репозиторию на хосте |
| `DOCKER_IMAGE_PREFIX` | `sensa` | Префикс имён образов |
| `NOTIFY_ENABLED` | `false` | Включает email-уведомления |
| `NOTIFY_EMAIL_RECIPIENTS` | — | Список получателей |

### Docker-сокет

Jenkins и агенты монтируют `/var/run/docker.sock`. На сервере его GID — `988`,
поэтому в `docker-compose.yml` задано `group_add: ["${DOCKER_GID:-988}"]`.
Если GID на другом сервере отличается:

```bash
stat -c '%g' /var/run/docker.sock
# задать DOCKER_GID=<gid> в jenkins/.env
```

### SSH с агента на хост

Агенты используют `extra_hosts: host.docker.internal:host-gateway`, чтобы
достучаться до SSH-демона хоста. Ключ `deploy` уже добавлен в
`authorized_keys`; `StrictHostKeyChecking` отключён для этого соединения.

## Мониторинг

После деплоя доступны:

- Grafana — `https://grafana.<BASE_DOMAIN>` (дашборды Node Exporter, контейнеры,
  сервисы, логи);
- Prometheus — `127.0.0.1:9090`;
- Loki — `127.0.0.1:3100` (логи контейнеров через Grafana Alloy).

Метрики приложений:

- Java/Kotlin — `/actuator/prometheus` (Micrometer);
- Rust — `/metrics` (`axum-prometheus`).

## Замечание по масштабированию

Сервисы одного типа делят одну метку агента. Пока у метки один executor,
параллельные ветки выполняются последовательно. Чтобы ускорить сборку,
добавь агентов с той же меткой или увеличь число executors у ноды.
