# План рефакторинга user-management-service → user-data-management-service

## Контекст

Сервис хранит все данные о пользователе. Другие микросервисы обращаются к нему через WebClient (REST) или Kafka.

- **Регистрация** — происходит в `authentication-service`, данные профиля приходят через Kafka (`user.registered`)
- **REST API** — только GET/PATCH/DELETE (создание только через Kafka)
- **Стек** — Reactive (Spring WebFlux + R2DBC)

---

## Фаза 1 — Удаление старого (38 элементов)

### Entity (4 source + 4 .class)
| Файл | Причина |
|------|---------|
| `data/entity/UserEntity.java` | Заменим обновлённой версией |
| `data/entity/UserMainDataEntity.java` | Заменим обновлённой версией |
| `data/entity/UserNotificationsSettingsEntity.java` | Заменим обновлённой версией |
| `data/entity/UserLocationDataEntity.java` | Заменим обновлённой версией |
| `build/.../Client.class` | Старый проект, JPA |
| `build/.../AdditionalUserInfo.class` | Старый проект, JPA |
| `build/.../EmergencyContact.class` | Старый проект, JPA |
| `build/.../SystemData.class` | Старый проект, JPA |

### Repository (4 source + 2 .class)
| Файл | Причина |
|------|---------|
| `data/repository/UserEntityRepository.java` | Заменим обновлённой версией |
| `data/repository/UserMainDataRepository.java` | Заменим обновлённой версией |
| `data/repository/UserNotificationSettingsRepository.java` | Заменим обновлённой версией |
| `data/repository/UserLocationDataRepository.java` | Заменим обновлённой версией |
| `build/.../ClientRepository.class` | Старый проект, JPA |
| `build/.../SystemDataRepository.class` | Старый проект, JPA |

### DTO (6 файлов)
| Файл |
|------|
| `dto/ClientRegistrationDto.java` |
| `dto/ClientResponse.java` |
| `dto/AdditionalUserInfoUpdateDto.java` |
| `dto/UserAuthenticationDto.java` |
| `dto/UserAuthenticationAnswerDto.java` |
| `dto/UserAuthenticationAnswer.java` |

### Service (3 файла)
| Файл |
|------|
| `service/AuthenticationService.java` |
| `service/ClientControllerMethods.java` |
| `service/SystemDataCleanupService.java` |

### Controller (1 файл)
| Файл |
|------|
| `controller/ClientController.java` |

### Kafka (2 файла)
| Файл |
|------|
| `kafka/AuthenticationServiceKafkaConsumer.java` |
| `kafka/AuthenticationServiceKafkaProducer.java` |

### Config/kafka (1 файл)
| Файл |
|------|
| `config/kafka/AuthenticationServiceKafkaConfiguration.java` |

### Serializer/Deserializer (2 файла-пакета)
| Файл |
|------|
| `serializer/UserAuthenticationAnswerDtoSerializer.java` |
| `deserializer/UserAuthenticationDtoDeserializer.java` |

### Model/Enum (3 файла)
| Файл |
|------|
| `model/Gender.java` |
| `model/PreferredCommunicationChannel.java` |
| `model/Role.java` |

### Exception (2 из 3)
| Файл | Причина |
|------|---------|
| `exception/ClientRegistrationException.java` | Старая логика |
| `exception/UserNotRegisteredException.java` | Старая логика |
| `exception/UserNotFoundException.java` | **ОСТАВИТЬ** — переиспользуем |

### Mapper (1 файл)
| Файл |
|------|
| `mapper/ClientMapper.java` |

### Task (1 файл)
| Файл |
|------|
| `task/ScheduledTasks.java` |

### Test (1 файл)
| Файл |
|------|
| `test/.../UserEntityManagementServiceApplicationTests.java` |

### Liquibase (1 файл)
| Файл |
|------|
| `db/changelog/changeset/user_V001_initial.sql` |

---

## Фаза 2 — Модификация существующего (6 файлов)

### `build.gradle`
- Удалить секции "Mongo Reactive" и "RabbitMQ Reactive"

### `settings.gradle`
- `rootProject.name = 'user-data-management-service'`

### `application.yaml`
- Убрать JDBC `datasource`, добавить `r2dbc`
- Добавить `spring.kafka.bootstrap-servers`, producer/consumer конфиг
- Добавить `spring.redis`
- Обновить `spring.application.name`, `springdoc` пути

### `SecurityConfig.java`
- Заменить `@EnableWebSecurity` + `HttpSecurity` на `@EnableWebFluxSecurity` + `ServerHttpSecurity`
- Убрать `BCryptPasswordEncoder` (пароли в auth-service)

### `docker-compose.yml`
- Переименовать сервис, добавить Redis, обновить DB name

### `db.changelog-master.yaml`
- Указать новый changelog файл

---

## Фаза 3 — Создание новых файлов (15)

### Entity (4 файла)
- `data/entity/UserEntity.java` — `id`, `userId` (UUID), `createdAt`, `updatedAt`
- `data/entity/UserMainDataEntity.java` — `firstName`, `lastName`, `email`, `phoneNumber`
- `data/entity/UserLocationDataEntity.java` — `country`, `city`, `street`, `homeNumber`
- `data/entity/UserNotificationsSettingsEntity.java` — `push`, `emailEnabled`, `sms` (+ добавить sms, переименовать email → emailEnabled)

### Repository (4 файла)
- `data/repository/UserEntityRepository.java`
- `data/repository/UserMainDataRepository.java`
- `data/repository/UserNotificationSettingsRepository.java` (обновить SELECT с sms/emailEnabled)
- `data/repository/UserLocationDataRepository.java`

### DTO (4 файла)
- `dto/UserCreateEvent.java` — Kafka event от auth-service
- `dto/UserUpdateRequest.java` — PATCH запрос (все поля optional)
- `dto/UserResponse.java` — полный ответ с notification settings
- `dto/NotificationSettingsDto.java` — push, emailEnabled, sms

### Service (1 файл)
- `service/UserService.java` — реактивный CRUD через 4 репозитория:
  - `createFromKafkaEvent(UserCreateEvent)` — создаёт 4 записи
  - `getByUserId(UUID)` — собирает UserResponse из 4 таблиц
  - `update(UUID, UserUpdateRequest)` — частичное обновление
  - `delete(UUID)` — каскадное удаление

### Controller (1 файл)
- `controller/UserController.java`:
  - `GET /api/v1/users/{userId}`
  - `PATCH /api/v1/users/{userId}`
  - `DELETE /api/v1/users/{userId}`

### Kafka (3 файла)
- `config/kafka/UserDataKafkaConfig.java` — producer/consumer factory
- `kafka/UserDataEventConsumer.java` — слушает `user.registered`
- `kafka/UserDataEventProducer.java` — публикует `user.updated`, `user.deleted`

### Mapper (1 файл)
- `mapper/UserMapper.java` — MapStruct

### Liquibase (1 файл-скрипт)
- `db/changelog/changeset/user-data_V001_initial.sql` — таблицы: users, user_main_data, user_location_data, user_notification_settings

### Test (1 файл)
- `test/.../UserDataManagementServiceApplicationTests.java`

---

## Схема таблиц

```
users
├── id (PK, BIGSERIAL)
├── user_id (UUID, UNIQUE)
├── created_at
└── updated_at

user_main_data (1:1 → users)
├── user_entity_id (PK, FK → users.id ON DELETE CASCADE)
├── first_name
├── last_name
├── email (UNIQUE)
├── phone_number
├── created_at
└── updated_at

user_location_data (1:1 → users)
├── user_entity_id (PK, FK → users.id ON DELETE CASCADE)
├── country
├── city
├── street
├── home_number
├── created_at
└── updated_at

user_notification_settings (1:1 → users)
├── user_entity_id (PK, FK → users.id ON DELETE CASCADE)
├── push (DEFAULT TRUE)
├── email_enabled (DEFAULT TRUE)
├── sms (DEFAULT TRUE)
├── created_at
└── updated_at
```

---

## Kafka топики

| Топик | Направление | Назначение |
|-------|-------------|------------|
| `user.registered` | Consumer (от auth-service) | Создание профиля при регистрации |
| `user.updated` | Producer | Оповещение других сервисов об изменении данных |
| `user.deleted` | Producer | Оповещение других сервисов об удалении |

---

## REST API

| Метод | Путь | Описание |
|-------|------|----------|
| `GET` | `/api/v1/users/{userId}` | Получить профиль пользователя |
| `PATCH` | `/api/v1/users/{userId}` | Обновить данные пользователя |
| `DELETE` | `/api/v1/users/{userId}` | Удалить пользователя |

---

## Порядок выполнения

```
1. Удалить все файлы из Фазы 1
2. gradle clean
3. Обновить build.gradle
4. Обновить settings.gradle
5. Перезаписать application.yaml
6. Обновить SecurityConfig.java
7. Обновить docker-compose.yml
8. Создать Liquibase changelog
9. Создать 4 Entity
10. Создать 4 Repository
11. Создать 4 DTO
12. Создать UserMapper
13. Создать UserService
14. Создать UserController
15. Создать 3 Kafka класса
16. Создать тесты
17. gradle build (проверка компиляции)
18. Запустить тесты
```
