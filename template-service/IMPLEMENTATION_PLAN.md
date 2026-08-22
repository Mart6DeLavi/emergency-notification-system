# План рефакторинга template-service (Java → Kotlin)

## Контекст

Сервис хранит шаблоны уведомлений (email, push, sms) для системы экстренного оповещения. Работники служб спасения создают шаблоны через веб-панель, а notification-service использует их для формирования сообщений.

- **Стек**: Kotlin + Spring Boot + JPA + PostgreSQL
- **Безопасность**: JWT (HS256, userId из токена)
- **Архитектурно**: CRUD-сервис, не занимается рендерингом

---

## Анализ текущего состояния

### Проблемы

| # | Проблема | Детали |
|---|----------|--------|
| 1 | **Java вместо Kotlin** | По SERVICE_ARCHITECTURE.md должен быть Kotlin |
| 2 | **`client_username` вместо `userId`** | Поле берётся из HTTP-заголовка, нет авторизации |
| 3 | **Ненужная таблица `username_template`** | Усложняет модель без пользы |
| 4 | **Поле `title` вместо `templateName`** | Семантически неверно для шаблонов |
| 5 | **Нет разделения по каналам** | Шаблоны для email/push/sms хранятся одинаково |
| 6 | **Баг в `TemplateMapper.update()`** | `setTitle(template.getTitle())` — использует старые данные вместо request |
| 7 | **`TemplateCreationExcetion`** | Опечатка в названии класса |
| 8 | **Нет аутентификации** | Любой может читать/писать шаблоны |
| 9 | **Нет поля `description`** | Для админ-панели нужно описание шаблона |
| 10 | **Нет валидации контента** | Шаблон без `{{placeholder}}` бесполезен |

### Текущие эндпоинты (будут полностью заменены)

```
POST   /api/v1/templates/create         — требует username в заголовке
GET    /api/v1/templates/find/{title}    — требует username в заголовке
PATCH  /api/v1/templates/update/{title}  — требует username в заголовке
DELETE /api/v1/templates/delete/{title}  — требует username в заголовке
```

---

## Целевая архитектура

```
Веб-панель служб спасения
        │
        │  JWT в Authorization: Bearer <token>
        │
        ▼
template-service (Kotlin)
        │
        │  Хранит шаблоны в PostgreSQL
        │  userId из JWT для audit trail (createdBy)
        │
        ▼
notification-service (читает шаблоны через Feign/WebClient)
        │
        │  GET /api/v1/templates/{templateName}
        │  Подставляет {{placeholders}} → рендерит сообщение
        │
        ▼
message-delivery-service (отправка email/push/sms)
```

---

## Фаза 1 — Полная очистка (весь src/main/java)

### Удалить

Весь Java-код в `src/main/java/com/sensa/templateservice/`:
- `TemplateServiceApplication.java`
- `controller/TemplateController.java`
- `service/TemplateService.java`
- `repository/TemplateRepository.java`
- `entity/Template.java`
- `mapper/TemplateMapper.java`
- `dto/TemplateRequest.java`
- `dto/TemplateResponse.java`
- `exception/TemplateAlreadyExistException.java`
- `exception/TemplateCreationExcetion.java`
- `exception/TemplateNotFoundException.java`

### Удалить тесты (будут переписаны)

- `src/test/` — все файлы:
  - `TemplateServiceApplicationTests.java`
  - `controller/TemplateControllerTest.java`
  - `controller/TemplateControllerAdditionalTest.java`
  - `service/TemplateServiceTest.java`
  - `service/TemplateServiceAdditionalTest.java`
  - `mapper/TemplateMapperTest.java`
  - `mapper/TemplateMapperAdditionalTest.java`

---

## Фаза 2 — Модификация существующих файлов (3 файла)

### `build.gradle` → `build.gradle.kts`

Переписать на Kotlin DSL:
- Плагины: `kotlin("jvm")`, `kotlin("plugin.spring")`, `kotlin("plugin.jpa")`, `spring-boot`, `spring-dependency-management`
- Зависимости: заменить Java-зависимости на Kotlin-эквиваленты
- Новая JWT-зависимость: `jjwt-api:0.12.6`, `jjwt-impl:0.12.6`, `jjwt-jackson:0.12.6`
- Убрать OpenFeign (template-service не ходит в другие сервисы)
- Убрать MapStruct (используем extension-функции Kotlin)
- Добавить `kotlin-reflect`, `jackson-module-kotlin`
- Добавить H2 для тестов

### `settings.gradle`

```groovy
rootProject.name = 'template-service'
```
— без изменений, имя правильное.

### `docker-compose.yml`

- Привести к единому стандарту (как в auth-service):
  - Порт `8003` (контейнер и хост)
  - Единая сеть `sensa-network`
  - Переменные окружения через `POSTGRES_USERNAME`, `POSTGRES_PASSWORD`, `JWT_SECRET`
  - Убрать лишнее

---

## Фаза 3 — Создание новых файлов (16 файлов на Kotlin)

### 3.1. Liquibase (2 файла)

#### `db/changelog/db.changelog-master.yaml`

```yaml
databaseChangeLog:
  - include:
      file: db/changelog/changeset/template_V001_initial.sql
```

#### `db/changelog/changeset/template_V001_initial.sql`

```sql
-- Удалить старые таблицы (если были)
DROP TABLE IF EXISTS username_template CASCADE;
DROP TABLE IF EXISTS template CASCADE;

CREATE TABLE templates (
    id BIGSERIAL PRIMARY KEY,
    template_name VARCHAR(255) NOT NULL UNIQUE,
    description VARCHAR(500),
    channel VARCHAR(10) NOT NULL CHECK (channel IN ('EMAIL', 'PUSH', 'SMS')),
    content TEXT NOT NULL,
    created_by UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_templates_channel ON templates(channel);
CREATE INDEX idx_templates_created_by ON templates(created_by);
```

**Изменения относительно старой схемы:**
- `client_username` → убран
- `title` → `template_name` (уникальный)
- Добавлены: `description`, `channel`, `created_by`, `created_at`, `updated_at`
- Убрана таблица `username_template`
- Добавлены индексы

### 3.2. Конфигурация (3 файла)

#### `config/SecurityConfig.kt`

```kotlin
@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val jwtAuthFilter: JwtAuthFilter
) {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .cors { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests {
                it.requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/template-service/api-docs/**").permitAll()
                it.requestMatchers("/actuator/health").permitAll()
                it.anyRequest().authenticated()
            }
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter::class.java)
        return http.build()
    }
}
```

**Логика**: Swagger + health-check открыты, всё остальное требует JWT.

#### `config/JwtTokenUtils.kt`

- Парсинг JWT без ключа (токен уже проверен auth-service)
- `@Value("\${jwt.secret}")` + SecretKey из HS256
- `fun parseToken(token: String): Claims`
- `fun getUserIdFromToken(token: String): UUID`
- `fun getEmailFromToken(token: String): String`
- `fun extractTokenFromHeader(header: String?): String?` — извлекает "Bearer xxx"

#### `config/JwtAuthFilter.kt`

`OncePerRequestFilter`:
1. Извлекает токен из `Authorization: Bearer <token>`
2. Парсит JWT через `JwtTokenUtils`
3. Устанавливает `SecurityContextHolder` с `UsernamePasswordAuthenticationToken`

### 3.3. Entity + Enum (2 файла)

#### `entity/Template.kt`

```kotlin
@Entity
@Table(name = "templates")
data class Template(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "template_name", nullable = false, unique = true)
    var templateName: String,

    @Column(name = "description", length = 500)
    var description: String? = null,

    @Column(name = "channel", nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    var channel: TemplateChannel,

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    var content: String,

    @Column(name = "created_by", nullable = false)
    var createdBy: UUID,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)
```

#### `entity/TemplateChannel.kt`

```kotlin
enum class TemplateChannel { EMAIL, PUSH, SMS }
```

### 3.4. Repository (1 файл)

#### `repository/TemplateRepository.kt`

```kotlin
interface TemplateRepository : JpaRepository<Template, Long> {
    fun findByTemplateName(templateName: String): Template?
    fun existsByTemplateName(templateName: String): Boolean
    fun findByChannel(channel: TemplateChannel): List<Template>
    fun deleteByTemplateName(templateName: String): Int
}
```

### 3.5. DTO (2 файла)

#### `dto/TemplateRequest.kt`

```kotlin
data class TemplateRequest(
    @field:NotBlank(message = "Template name cannot be blank")
    val templateName: String,

    val description: String?,

    @field:NotNull(message = "Channel cannot be null")
    val channel: TemplateChannel,

    @field:NotBlank(message = "Content cannot be blank")
    val content: String
)
```

#### `dto/TemplateResponse.kt`

```kotlin
data class TemplateResponse(
    val id: Long,
    val templateName: String,
    val description: String?,
    val channel: TemplateChannel,
    val content: String,
    val createdBy: UUID,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)
```

### 3.6. Mapper (1 файл)

#### `mapper/TemplateMapper.kt`

Extension-функции вместо MapStruct:

```kotlin
fun TemplateRequest.toEntity(createdBy: UUID): Template = Template(
    templateName = templateName,
    description = description,
    channel = channel,
    content = content,
    createdBy = createdBy
)

fun Template.toResponse(): TemplateResponse = TemplateResponse(
    id = id,
    templateName = templateName,
    description = description,
    channel = channel,
    content = content,
    createdBy = createdBy,
    createdAt = createdAt,
    updatedAt = updatedAt
)
```

### 3.7. Service (1 файл)

#### `service/TemplateService.kt`

```kotlin
@Service
class TemplateService(
    private val templateRepository: TemplateRepository
) {
    fun create(request: TemplateRequest, createdBy: UUID): TemplateResponse {
        if (templateRepository.existsByTemplateName(request.templateName)) {
            throw TemplateAlreadyExistsException(request.templateName)
        }
        val entity = request.toEntity(createdBy)
        return templateRepository.save(entity).toResponse()
    }

    fun getByTemplateName(templateName: String): TemplateResponse {
        return templateRepository.findByTemplateName(templateName)?.toResponse()
            ?: throw TemplateNotFoundException(templateName)
    }

    fun getAll(): List<TemplateResponse> {
        return templateRepository.findAll().map { it.toResponse() }
    }

    fun getByChannel(channel: TemplateChannel): List<TemplateResponse> {
        return templateRepository.findByChannel(channel).map { it.toResponse() }
    }

    fun update(templateName: String, request: TemplateRequest): TemplateResponse {
        val existing = templateRepository.findByTemplateName(templateName)
            ?: throw TemplateNotFoundException(templateName)

        existing.templateName = request.templateName
        existing.description = request.description
        existing.channel = request.channel
        existing.content = request.content
        existing.updatedAt = LocalDateTime.now()

        return templateRepository.save(existing).toResponse()
    }

    fun delete(templateName: String): Boolean {
        val rows = templateRepository.deleteByTemplateName(templateName)
        if (rows == 0) throw TemplateNotFoundException(templateName)
        return true
    }
}
```

### 3.8. Controller (1 файл)

#### `controller/TemplateController.kt`

```kotlin
@RestController
@RequestMapping("/api/v1/templates")
class TemplateController(
    private val templateService: TemplateService
) {
    @PostMapping
    fun create(
        @RequestBody @Valid request: TemplateRequest,
        authentication: JwtAuthenticationToken
    ): ResponseEntity<TemplateResponse> {
        val userId = authentication.token.claims["userId"] as String
        return ResponseEntity.status(201).body(templateService.create(request, UUID.fromString(userId)))
    }

    @GetMapping
    fun getAll(): ResponseEntity<List<TemplateResponse>> {
        return ResponseEntity.ok(templateService.getAll())
    }

    @GetMapping("/{templateName}")
    fun getByTemplateName(@PathVariable templateName: String): ResponseEntity<TemplateResponse> {
        return ResponseEntity.ok(templateService.getByTemplateName(templateName))
    }

    @GetMapping("/channel/{channel}")
    fun getByChannel(@PathVariable channel: TemplateChannel): ResponseEntity<List<TemplateResponse>> {
        return ResponseEntity.ok(templateService.getByChannel(channel))
    }

    @PatchMapping("/{templateName}")
    fun update(
        @PathVariable templateName: String,
        @RequestBody @Valid request: TemplateRequest
    ): ResponseEntity<TemplateResponse> {
        return ResponseEntity.ok(templateService.update(templateName, request))
    }

    @DeleteMapping("/{templateName}")
    fun delete(@PathVariable templateName: String): ResponseEntity<Boolean> {
        return ResponseEntity.ok(templateService.delete(templateName))
    }
}
```

### 3.9. Exception + Handler (4 файла)

#### `exception/TemplateAlreadyExistsException.kt`
#### `exception/TemplateNotFoundException.kt`
#### `exception/TemplateValidationException.kt` — на случай шаблона без `{{placeholder}}`
#### `exception/GlobalExceptionHandler.kt` — `@RestControllerAdvice`, маппит исключения на HTTP-статусы

### 3.10. Главный класс (1 файл)

#### `TemplateServiceApplication.kt`

```kotlin
@SpringBootApplication
@EnableDiscoveryClient
class TemplateServiceApplication

fun main(args: Array<String>) {
    runApplication<TemplateServiceApplication>(*args)
}
```

Без `@EnableFeignClients` — template-service никого не вызывает.

### 3.11. `application.yaml`

```yaml
spring:
  application:
    name: template-service

  datasource:
    url: jdbc:postgresql://localhost:5432/Sensa_Template_Service
    username: ${POSTGRES_USERNAME}
    password: ${POSTGRES_PASSWORD}
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: validate

  liquibase:
    change-log: classpath:db/changelog/db.changelog-master.yaml

eureka:
  client:
    serviceUrl:
      defaultZone: http://localhost:8761/eureka/
    fetchRegistry: true
    registerWithEureka: true

server:
  port: 8003

jwt:
  secret: ${JWT_SECRET}

springdoc:
  api-docs:
    path: /template-service/api-docs
  swagger-ui:
    path: /swagger-ui.html
    operations-sorter: method
```

---

## Фаза 4 — Тесты (Kotlin)

### `TemplateServiceTest.kt`

Unit-тесты с MockK:
- `createTemplate_Success` — создаёт шаблон, возвращает response
- `createTemplate_AlreadyExists` — бросает TemplateAlreadyExistsException
- `getByTemplateName_Success` — находит по имени
- `getByTemplateName_NotFound` — бросает TemplateNotFoundException
- `getAll` — возвращает список
- `getByChannel` — фильтр по EMAIL
- `update_Success` — обновляет поля
- `update_NotFound` — бросает исключение
- `delete_Success` — удаляет, возвращает true
- `delete_NotFound` — бросает исключение

### `TemplateControllerTest.kt`

`@WebMvcTest` с MockMvc:
- `POST /api/v1/templates` → 201
- `POST /api/v1/templates` с дубликатом → 409
- `GET /api/v1/templates` → 200 + список
- `GET /api/v1/templates/{name}` → 200
- `GET /api/v1/templates/{name}` не найден → 404
- `GET /api/v1/templates/channel/EMAIL` → 200 + фильтр
- `PATCH /api/v1/templates/{name}` → 200
- `DELETE /api/v1/templates/{name}` → 200

### `JwtTokenUtilsTest.kt`

- Генерация/парсинг/валидация токена
- Извлечение userId
- Просроченный токен
- Неверная подпись

---

## Итоговые эндпоинты

| Метод | Путь | Тело | Ответ | Статус |
|-------|------|------|-------|--------|
| `POST` | `/api/v1/templates` | `TemplateRequest` | `TemplateResponse` | 201 |
| `GET` | `/api/v1/templates` | — | `List<TemplateResponse>` | 200 |
| `GET` | `/api/v1/templates/{templateName}` | — | `TemplateResponse` | 200 |
| `GET` | `/api/v1/templates/channel/{channel}` | — | `List<TemplateResponse>` | 200 |
| `PATCH` | `/api/v1/templates/{templateName}` | `TemplateRequest` | `TemplateResponse` | 200 |
| `DELETE` | `/api/v1/templates/{templateName}` | — | `Boolean` | 200 |

Все эндпоинты требуют JWT в заголовке `Authorization: Bearer <token>`.

---

## Схема БД

```sql
templates
├── id           BIGSERIAL PK
├── template_name VARCHAR(255) UNIQUE NOT NULL
├── description  VARCHAR(500)
├── channel      VARCHAR(10) NOT NULL CHECK (EMAIL | PUSH | SMS)
├── content      TEXT NOT NULL
├── created_by   UUID NOT NULL
├── created_at   TIMESTAMP NOT NULL DEFAULT NOW()
└── updated_at   TIMESTAMP NOT NULL DEFAULT NOW()
```

---

## `build.gradle.kts` — зависимости

```kotlin
dependencies {
    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // JWT (jjwt 0.12.x)
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

    // Database
    runtimeOnly("org.postgresql:postgresql")
    implementation("org.liquibase:liquibase-core")

    // Spring Cloud
    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client")

    // OpenAPI
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.7.0")

    // Dev + Test
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testRuntimeOnly("com.h2database:h2")
    testImplementation("io.mockk:mockk:1.13.12")
    testImplementation("com.ninja-squad:springmockk:4.0.2")
}
```

---

## Порядок выполнения

```
1. Очистить src/main/java + src/test
2. Переписать build.gradle → build.gradle.kts
3. Обновить Liquibase V001 (template → templates)
4. Создать config/ (SecurityConfig, JwtTokenUtils, JwtAuthFilter)
5. Создать entity/ (Template, TemplateChannel)
6. Создать dto/ (TemplateRequest, TemplateResponse)
7. Создать mapper/ (TemplateMapper — extension functions)
8. Создать repository/ (TemplateRepository)
9. Создать service/ (TemplateService)
10. Создать exception/ + GlobalExceptionHandler
11. Создать controller/ (TemplateController)
12. Создать TemplateServiceApplication.kt
13. Обновить application.yaml
14. Обновить docker-compose.yml
15. Создать тесты (Service + Controller + JWT)
16. gradle clean build
```
