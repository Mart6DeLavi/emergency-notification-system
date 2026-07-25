# Архитектура Сервисов (Microservices Architecture)

Данный документ содержит описание микросервисов, их структур данных, назначения и используемого стек технологий на основе представленной схемы.

---

## 1. user-data-management-service

**Назначение:** Отвечает за хранение данных о пользователе.  
**Стек технологий:** `Java`

### Структура данных (Поля):
1. `Id`
2. `userId`
3. `firstName`
4. `LastName`
5. `email`
6. `phoneNumber`
7. `Country`
8. `City`
9. `Street`
10. `HomeNumber`
11. `CreatedAt`
12. `UpdatedAt`
13. `UserNotificationSettings`

---

## 2. authentication-service

**Назначение:** Хранит логин и пароль пользователя, чтобы работники экстренных служб спасения могли входить в свои аккаунты.  
**Стек технологий:** `Java`

### Структура данных (Поля):
1. `Id`
2. `email`
3. `password`

---

## 3. emergency-situation-request-service

**Назначение:** Хранит историю ситуаций, которые произошли и о которых либо пользователи объявляют, либо службы спасения.  
**Стек технологий:** `Rust`

### Структура данных (Поля):
1. `Id`
2. `userId`
3. `title`
4. `description`
5. `files`
6. `Country`
7. `City`
8. `Street`
9. `AlarmTimestamp`
10. `CreatedAt`

---

## 4. message-delivery-service

**Назначение:** Отправляет уведомления пользователям.  
**Стек технологий:** `Rust`

### Структура данных (Поля):
1. `Id`
2. `templateId`
3. `messageBody`
4. `CreatedAt`

---

## 5. filesystem-service

**Назначение:** Хранит всю информацию о файлах и к какому происшествию оно относится.  
**Стек технологий:** `Kotlin`

### Структура данных (Поля):
1. `Id`
2. `userId`
3. `URL`
4. `CreatedAt`
5. `EmergencySituationId`

---

## 6. template-service

**Назначение:** Хранит шаблоны уведомлений для email, push.  
**Стек технологий:** `Kotlin`

### Структура данных (Поля):
1. `Id`
2. `desctiption` *(опечатка в схеме)*
3. `templateName`
4. `CreatedAt`

---

## 7. notification-service

**Назначение:** Склеивает шаблоны с текстом от пользователей.  
**Стек технологий:** `Java`

---

## 8. Вспомогательные сервисы / Инфраструктура

- **api-gateway** — Входная точка / шлюз API.
- **discovery-server** — Сервер обнаружения сервисов (Service Discovery).
