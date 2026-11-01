# План: строгий Security-гейт + обновление зависимостей (Вариант B)

**Дата:** 2026-11-01  
**Статус:** запланировано  
**Ветка:** `security-deps` (предлагаемая)  
**Связанный билд:** Jenkins `sensa` #5 (`FAILURE`, 36.5 мин)

---

## 1. Контекст

В build #5:

- **5 Rust-сервисов** (`emergency-situation-request-service`, `message-delivery-service`, `load-balancer-service`, `rate-limiter-service`, `api-gateway`) прошли все стадии.
- **6 Java/Kotlin-сервисов** прошли **Quality** и **Build**, но упали на **Security**.
- Причина — Trivy нашёл **исправимые** HIGH/CRITICAL CVE в зависимостях внутри `app.jar`
  (Spring Boot 3.4.1/3.4.3, Netty, Spring Security, Jackson, BouncyCastle и т.д.).
- Так как Security упал, стадии **Deploy** и **Rollout** не запускались.

Выбран **Вариант B**: не ослаблять гейт, а обновить зависимости так, чтобы образы проходили
Trivy при `--severity HIGH,CRITICAL --ignore-unfixed --exit-code 1`.

---

## 2. Цель и критерии приёмки

**Цель:** шесть Java/Kotlin-сервисов собираются и проходят Security-стадию без исправимых
HIGH/CRITICAL CVE; гейт остаётся блокирующим.

**Критерии приёмки:**

1. `./gradlew clean build` зелёный для всех 6 сервисов.
2. Docker-образы собираются.
3. Trivy для каждого образа: **0** исправимых HIGH/CRITICAL.
4. Jenkins build #6: `Quality → Build → Security → Deploy → Rollout` — зелёный.
5. Rollout поднимает стек, Caddy получает сертификаты.
6. `--exit-code 1` и `--ignore-unfixed` в `jenkins/vars/security.groovy` **сохраняются**.

---

## 3. Затронутые сервисы

| Сервис | Сборка | Spring Boot | Примечание |
|---|---|---|---|
| `authentication-service` | Gradle (Java) | 3.4.1 | JPA, Kafka, jjwt, Liquibase |
| `discovery-server` | Gradle (Java) | 3.4.3 | Eureka server |
| `filesystem-service` | Gradle (Kotlin) | 3.4.1 | S3, DJL, Kafka |
| `notification-service` | Gradle (Java) | 3.4.3 | Mongo, Redis, Kafka, RabbitMQ |
| `template-service` | Gradle (Kotlin) | 3.4.1 | Mongo, Redis |
| `user-management-service` | Gradle (Java) | 3.4.1 | WebFlux + R2DBC |

Rust-сервисы **не трогаем** — они уже проходят Security.

---

## 4. Найденные CVE и целевые версии

> «Min fixed» — минимальная версия из отчёта Trivy. На практике берём самый свежий патч в той же
> minor-линии, совместимый с Spring Boot 3.4.x, и проверяем `dependencyInsight`.

| Компонент | Сейчас | Min fixed | Severity | Как обновлять |
|---|---|---|---|---|
| Netty (`netty-handler`) | 4.1.116 | **4.1.133.Final** | CRITICAL (CVE-2026-75595) | `netty.version` |
| Spring Security core | 6.4.2 | **6.4.6** | CRITICAL (CVE-2025-41232) | `spring-security.version` |
| spring-security-web | 6.4.x | **6.4.6+** | CRITICAL (CVE-2026-22732) | `spring-security.version` |
| Spring Framework (`spring-core`/`spring-webflux`) | 6.2.1 | **6.2.11** | CRITICAL (CVE-2026-47890) | `spring-framework.version` |
| BouncyCastle | 1.78.1 | **1.80.2+** | CRITICAL (CVE-2025-14813) | `bouncycastle.version` |
| Jackson | 2.18.x | **2.18.11+** | HIGH | `jackson-bom.version` |
| Guava | 33.3.1 | **33.7.2** | HIGH | `guava.version` |
| XStream | 1.4.20 | **1.4.21** | HIGH | `xstream.version` |
| Micrometer | 1.14.2 | **1.15.12 / 1.16.6** | HIGH | `micrometer.version` |
| kafka-clients | 3.8.1 | **3.9.2** | HIGH | `kafka.version` |
| spring-kafka | 3.3.1 | **3.3.16** | HIGH | `spring-kafka.version` |
| httpclient5 | 5.4.1 | **5.4.3** | HIGH | `httpclient5.version` |
| lz4 | 1.8.0 | **1.8.1** | HIGH | `lz4.version` |
| snappy | 1.1.10.5 | **1.1.10.9** | HIGH | `snappy.version` |
| Spring Boot | 3.4.1 / 3.4.3 | **3.4.x latest** | HIGH | Gradle plugin + BOM |

---

## 5. Порядок работ

### Фаза 0 — База
1. Ветка `security-deps` от `master`.
2. Снять baseline-отчёты Trivy по 6 сервисам (сохранить в `docs/security/baseline/`).
3. Зафиксировать текущие версии через `./gradlew dependencyInsight`.

### Фаза 1 — Patch Spring Boot
1. Обновить Gradle-плагин `org.springframework.boot` и BOM до последнего 3.4.x
   (проверить наличие на Maven Central; ожидаемо 3.4.13+).
2. Spring Cloud оставить `2024.0.0` (совместим с Boot 3.4.x).
3. `./gradlew clean build` + Trivy по каждому сервису.

### Фаза 2 — Точечные overrides
1. Добавить `ext`-свойства (см. §6) в `build.gradle` / `build.gradle.kts`.
2. Пересобрать, проверить `dependencyInsight`, прогнать Trivy.
3. Повторять, пока не уйдут исправимые HIGH/CRITICAL.

### Фаза 3 — Остатки
1. Если CVE приходит из Liquibase (XStream/Guava) — обновить `liquibase-core`.
2. Если из Kafka — согласовать `spring-kafka` и `kafka-clients` (Boot-managed пара).
3. Если CVE без совместимого фикса — только тогда `.trivyignore` с обоснованием (крайняя мера).

### Фаза 4 — Jenkins
1. `jenkins/vars/security.groovy` оставить строгим:
   `--severity HIGH,CRITICAL --ignore-unfixed --exit-code 1`.
2. При необходимости — `--timeout` и кэш Trivy.

### Фаза 5 — Проверка
1. Commit → push `security-deps` → merge `--no-ff` → push `master`.
2. На сервере `git fetch/reset`, запустить build #6.
3. Дождаться `Rollout`, проверить `panel` / `jenkins` / `grafana`.

---

## 6. Технические детали (Gradle)

### Groovy (`build.gradle`)
```groovy
ext {
    set('netty.version', '4.1.133.Final')
    set('spring-security.version', '6.4.6')
    set('spring-framework.version', '6.2.11')
    set('jackson-bom.version', '2.18.11')
    set('bouncycastle.version', '1.80.2')
    set('kafka.version', '3.9.2')
    set('spring-kafka.version', '3.3.16')
    set('httpclient5.version', '5.4.3')
    set('guava.version', '33.7.2')
    set('xstream.version', '1.4.21')
    set('micrometer.version', '1.15.12')
    set('snappy.version', '1.1.10.9')
    set('lz4.version', '1.8.1')
}
```

Свойства должны быть заданы **до** применения Spring Boot BOM
(`dependencyManagement { imports { mavenBom ... } }`).

### Kotlin DSL (`build.gradle.kts`)
```kotlin
extra["netty.version"] = "4.1.133.Final"
extra["spring-security.version"] = "6.4.6"
extra["spring-framework.version"] = "6.2.11"
extra["jackson-bom.version"] = "2.18.11"
extra["bouncycastle.version"] = "1.80.2"
extra["kafka.version"] = "3.9.2"
extra["spring-kafka.version"] = "3.3.16"
extra["httpclient5.version"] = "5.4.3"
extra["guava.version"] = "33.7.2"
extra["xstream.version"] = "1.4.21"
extra["micrometer.version"] = "1.15.12"
extra["snappy.version"] = "1.1.10.9"
extra["lz4.version"] = "1.8.1"
```

### Принудительное разрешение (если BOM не покрывает транзитив)
```groovy
configurations.all {
    resolutionStrategy {
        force 'io.netty:netty-handler:4.1.133.Final'
    }
}
```

### Проверка версий
```bash
./gradlew dependencyInsight --dependency netty-handler --configuration runtimeClasspath
./gradlew dependencyInsight --dependency spring-security-core --configuration runtimeClasspath
```

---

## 7. Локальная проверка Trivy

```bash
docker build -t sensa/<service>:test ./<service>
docker run --rm \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -v trivy_cache:/root/.cache \
  aquasec/trivy:latest image \
  --severity HIGH,CRITICAL --ignore-unfixed \
  sensa/<service>:test
```

**Критерий:** в таблице нет строк с непустым «Fixed Version».

---

## 8. Риски и митигация

| Риск | Митигация |
|---|---|
| Boot 3.5 несовместим с Spring Cloud 2024.0.0 | Остаёмся на 3.4.x; 3.5 — только с Cloud 2025.0.x |
| Jackson 2.19 ломает Redis `GenericJackson2JsonRedisSerializer` | Предпочесть 2.18.11; проверить round-trip кэша |
| Netty override конфликтует с Boot | Переопределять только `netty-bom`/`netty.version`; проверять `dependencyInsight` |
| Spring Security override ломает auth | Прогнать register/login/validate и JWT-тесты |
| BouncyCastle конфликтует с jjwt | jjwt 0.12.6 использует свои провайдеры; прогнать тесты токенов |
| kafka-clients 3.9.2 несовместим с spring-kafka 3.3.x | Использовать пару, управляемую Boot, либо spring-kafka 3.3.16 |
| Guava/XStream приходят из Liquibase | Обновить `liquibase-core` до свежей 4.x |
| Тесты падают после апгрейда | Исправлять код/тесты; не отключать тесты |

---

## 9. Откат

- Изменения по сервисам изолированы; откат — revert коммитов.
- Если апгрейд блокирует защиту/поведение — временно вернуться к **Варианту A**
  (OS-гейт блокирующий, app-зависимости — отчёт), зафиксировать как техдолг.

---

## 10. Вне scope

- Rust-сервисы (уже проходят).
- OS-пакеты без фикса (`--ignore-unfixed`).
- Пиннинг base-image по digest (можно позже).
- Полный переход на Boot 3.5 / Cloud 2025.

---

## 11. Визуализация пайплайна (после зелёного билда)

Делаем только то, что реально помогает следить за 11 сервисами. Без кастомных дашбордов.

1. **Опции `pipeline-graph-view`** (Manage Jenkins → Appearance / конфиг плагина):
   - `showGraphOnJobPage`, `showStageNames`, `showStageDurations`, `showGraphOnBuildPage`.
2. **`timestamps` + `ansiColor('xterm')`** — пайплайн scripted, поэтому обёртки:
   ```groovy
   timestamps {
       ansiColor('xterm') {
           // ...
       }
   }
   ```
3. **Описание билда**: `currentBuild.description = "#${env.BUILD_NUMBER} · <ok>/11 ✅ · <duration> · ${env.BRANCH_NAME}@<sha>"`.
4. **JUnit для Gradle-сервисов**: `junit allowEmptyResults: true, testResults: '**/build/test-results/test/*.xml'` → вкладка Tests.
5. **Финальная стадия `📊 Summary`**: таблица `service → Quality/Build/Security/Deploy → время → image`; после Rollout — `docker compose ps` + health-check, чтобы видеть, что стек реально поднялся.

**Не делаем:** кастомный HTML-dashboard с табами/сайдбаром, badge-плагин, Trivy HTML, Warnings NG, Grafana deployment dashboard, разделение на 11 job'ов.

## 12. Чек-лист

- [ ] Ветка `security-deps`
- [ ] Baseline Trivy-отчёты
- [ ] Spring Boot patch обновлён
- [ ] Overrides добавлены (Netty, Security, Framework, Jackson, BouncyCastle, Kafka, …)
- [ ] `./gradlew clean build` — 6/6 зелёные
- [ ] Trivy — 0 исправимых HIGH/CRITICAL
- [ ] Commit → push → merge → push master
- [ ] Build #6 зелёный (Quality → Build → Security → Deploy → Rollout)
- [ ] `panel` / `jenkins` / `grafana` доступны
- [ ] Документация обновлена
