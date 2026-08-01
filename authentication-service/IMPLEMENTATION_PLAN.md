# План рефакторинга authentication-service

## Контекст

Сервис отвечает за регистрацию и аутентификацию пользователей. При регистрации сохраняет `email` + `BCrypt(password)` локально, генерирует `userId` (UUID), и через Kafka отправляет профильные данные в `user-data-management-service`.

- **Стек**: Spring Boot (Servlet, не Reactive) + JPA + PostgreSQL + Redis
- **Безопасность**: BCrypt + JWT + rate limiting (bucket4j)

---

## Текущие проблемы

| # | Проблема |
|---|----------|
| 1 | `username` вместо `email` в AuthEntity |
| 2 | Один endpoint делает всё: сохраняет, шлёт Kafka, ждёт ответ — нет разделения register/login |
| 3 | Kafka-поток наоборот: ждёт ответ от user-management-service, хотя сам только что сохранил |
| 4 | Пароль не хешируется — маппер тупо копирует plaintext |
| 5 | `jjwt:0.9.1` (2018) — устаревшая JWT библиотека |
| 6 | Redis-бэкап в PostgreSQL + `RedisRecoveryService` + `@Scheduled` — избыточно |
| 7 | Регистрация не принимает профильные данные (firstName, lastName, phone, адрес) |

---

## Фаза 1 — Удаление старого (14 + Liquibase)

| Файл | Причина |
|------|---------|
| `dto/UserAuthenticationAnswer.java` | Старый enum FOUND/NOTEXIST |
| `dto/UserAuthenticationAnswerDto.java` | Обёртка над enum |
| `dto/RedisBackupDto.java` | Ненужный Redis-бэкап |
| `entity/RedisBackupEntity.java` | Ненужная JPA-сущность |
| `repository/RedisBackupRepository.java` | Ненужный репозиторий |
| `kafka/UserManagementServiceKafkaProducer.java` | Старый Kafka-поток |
| `kafka/AsyncExecutorConfig.java` | Старый конфиг executor |
| `config/kafka/UserManagementServiceKafkaConfiguration.java` | Старый Kafka-конфиг |
| `deserializer/UserAuthenticationAnswerDtoDeserializer.java` | Старый десериалайзер |
| `serializer/UserAuthenticationDtoSerializer.java` | Старый сериалайзер |
| `service/RedisRecoveryService.java` | Избыточный Redis-бэкап |
| `mapper/RedisBackupEntityMapper.java` | Ненужный маппер |
| `mapper/UserAuthenticationDtoMapper.java` | Заменён сервисной логикой |
| Liquibase V002 `redis_backup_table_creation_V002.sql` | Убрать таблицу redis_backup |
| Liquibase V003 `redis_backup_V003.sql` | Убрать ALTER TABLE |

---

## Фаза 2 — Модификация (10 файлов)

### `AuthEntity.java`
- `username` → `email` (уникальный)
- Добавить `userId` (UUID, уникальный)

### `UserStorageRepository.java`
- `Optional<AuthEntity> findByEmail(String email)`
- `boolean existsByEmail(String email)`

### `dto/UserAuthenticationDto.java` → переименовать в `dto/LoginRequest.java`
- Оставить `email`, `password` с валидацией

### `AuthenticationController.java`
- Три эндпоинта вместо одного
- `POST /api/v1/auth/register` — регистрация
- `POST /api/v1/auth/login` — вход
- `POST /api/v1/auth/validate` — проверка токена

### `application.yaml`
- Убрать старые Kafka consumer настройки (только producer)
- Обновить JWT переменные

### `build.gradle`
- `jjwt:0.9.1` → `jjwt-api:0.12.6`, `jjwt-impl:0.12.6`, `jjwt-jackson:0.12.6`
- Убрать `jedis` (используем Lettuce из spring-boot-starter-data-redis)
- Убрать `jaxb-api`, `jaxb-runtime` (не нужны с новым jjwt)
- Убрать `hibernate-validator:6.2.5.Final` (Spring Boot сам тянет)

### `SecurityConfig.java`
- permitAll на `/api/v1/auth/**`
- authenticated на всё остальное
- Добавить `JwtAuthFilter` перед `UsernamePasswordAuthenticationFilter`
- Оставить `RateLimitingFilter`

### `BasicSecurityConfig.java`
- Только `BCryptPasswordEncoder`, убрать `AuthenticationManager`

### `JwtTokenUtils.java`
- Переписать под `io.jsonwebtoken` (0.12.x API)
- Методы: `generateToken(userId, email)`, `validateToken(token)`, `getUserId(token)`, `getEmail(token)`

### Liquibase V001
- `authentication_user`: `username` → `email` (уникальный), добавить `user_id UUID UNIQUE`

### `docker-compose.yml`
- Убрать `JWT_USER_SECRET_LIFETIME` (зашито в код), убрать Kafka consumer group
- Обновить переменные окружения

---

## Фаза 3 — Создание новых файлов (12)

### DTO (6)
- **`dto/RegisterRequest.java`** — `email`, `password`, `firstName`, `lastName`, `phoneNumber`, `country`, `city`, `street`, `homeNumber` + `@Valid`
- **`dto/LoginRequest.java`** — `email`, `password` + `@Valid`
- **`dto/AuthResponse.java`** — `token`, `userId`, `email`, `expiresAt`
- **`dto/TokenValidationRequest.java`** — `token`
- **`dto/TokenValidationResponse.java`** — `valid`, `userId`, `email`
- **`dto/UserRegisteredEvent.java`** — `userId`, `email`, `firstName`, `lastName`, `phoneNumber`, `country`, `city`, `street`, `homeNumber`

### Service (1)
- **`service/AuthService.java`**:
  - `register(RegisterRequest)` — генерирует UUID, BCrypt(password), сохраняет AuthEntity, шлёт `user.registered` в Kafka, возвращает `AuthResponse`
  - `login(LoginRequest)` — ищет по email, сверяет BCrypt, генерирует JWT, возвращает `AuthResponse`
  - `validate(String token)` — парсит JWT, возвращает `TokenValidationResponse`

### Kafka (2)
- **`kafka/UserRegisteredEventProducer.java`** — `KafkaTemplate<String, UserRegisteredEvent>`, отправка в `user.registered`
- **`config/kafka/KafkaProducerConfig.java`** — только `ProducerFactory` + `KafkaTemplate` (консьюмер не нужен)

### Security (1)
- **`config/security/JwtAuthFilter.java`** — `OncePerRequestFilter`, извлекает JWT из `Authorization: Bearer`, устанавливает `SecurityContext` (замена `RequestFilter`)

### Test (2)
- **`AuthServiceTest.java`** — unit-тесты register/login/validate
- **`AuthenticationControllerTest.java`** — интеграционные тесты эндпоинтов

---

## Эндпоинты

| Метод | Путь | Тело | Ответ | Статус |
|-------|------|------|-------|--------|
| `POST` | `/api/v1/auth/register` | `RegisterRequest` | `AuthResponse` (JWT + userId) | 201 |
| `POST` | `/api/v1/auth/login` | `LoginRequest` | `AuthResponse` (JWT + userId) | 200 |
| `POST` | `/api/v1/auth/validate` | `TokenValidationRequest` | `TokenValidationResponse` | 200 |

---

## Kafka

| Топик | Направление | Событие |
|-------|-------------|---------|
| `user.registered` | Producer → user-data-management-service | `UserRegisteredEvent` |

---

## Схема БД

```sql
CREATE TABLE authentication_user (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL
);
```

---

## Безопасность

- **Пароли**: BCrypt, сложность через `@Size(min = 8)` + доп. проверки
- **JWT**: HS256, claims: `userId`, `email`, `sub`, `iat`, `exp` (TTL 24h)
- **Rate limiting**: 10 запросов/мин/IP (bucket4j, оставляем)
- **CSRF**: выключен (REST API)
- **CORS**: настроен для мобильных/веб-клиентов

---

## Порядок выполнения

```
1. Удалить старые файлы (14 + Liquibase)
2. gradle clean
3. Обновить build.gradle
4. Обновить Liquibase V001
5. Обновить AuthEntity + UserStorageRepository
6. Обновить application.yaml
7. Обновить SecurityConfig + BasicSecurityConfig
8. Создать JwtAuthFilter (замена RequestFilter)
9. Переписать JwtTokenUtils
10. Создать DTO (6 штук)
11. Создать AuthService
12. Создать Kafka конфиг + продюсер
13. Обновить AuthenticationController
14. Удалить старые DTO/сервисы
15. Обновить docker-compose.yml
16. Обновить тесты
17. gradle build
```
