# 🚨 Emergency Notification System

> A flexible and scalable emergency alerting system designed to deliver critical notifications to recipients via Telegram. The system supports bulk import of recipients, predefined templates, and real-time message delivery.

## 📌 Overview

The Emergency Notification System is built to help organizations, institutions, or communities quickly notify people during emergencies such as natural disasters, system outages, or public safety threats. The system is currently focused on delivering alerts via **Telegram**, with plans to expand support to other communication channels like Email, SMS, and Push Notifications.

It features an admin-friendly interface for managing recipients, importing large datasets, and creating reusable templates to streamline the notification process.

---

## ✅ Functional Requirements

### 🔔 Notification Sending

The system allows authorized users to send emergency alerts to all or specific registered recipients. These messages are currently delivered through:
- ⬜ **Email** — Planned for future releases.
- ⬜ **SMS** — To be integrated with external SMS gateways.

---

### 🧾 Notification Templates

Users can create and manage templates for messages they need to send repeatedly. This greatly speeds up the process during urgent situations by avoiding the need to type out the same content multiple times.

Each template includes:
- A title (label for identification)
- Message body (can include variables in future versions)
- Creation/modification timestamps

---

### 📨 Recipient Response Capability

⬜ **Planned feature.**  
Recipients will eventually be able to respond to messages — for example, confirming they are safe or requesting help. These responses can then be monitored in a dashboard.

---

## ⚙️ Non-Functional Requirements

### 🟢 High Availability
⬜ Not yet achieved, but planned through containerization and load-balancing in future releases.

### 🟢 Reliability
✅ Telegram delivery is stable and reliable. The system retries failed deliveries and provides logs for traceability.

### 🟢 Low Latency
✅ Notifications are delivered almost instantly thanks to asynchronous processing and lightweight architecture.

### 🟢 Scalability
✅ Built with Spring Boot and PostgreSQL, the system supports horizontal scaling and efficient database operations for large datasets.

### 🟢 Security
✅ Sensitive operations are protected via authorization keys. Incoming imports and user data are validated and sanitized to prevent unauthorized access or data corruption.

---

## ✨ Additional Features

- **Email Update Guarantee**  
  When importing recipients, if an email already exists in the database, the corresponding record will be updated with the latest data from the import file. This ensures data consistency and avoids duplication.

---

## 🧰 Tech Stack

- **Java 21** — Modern language features for clean and safe code.
- **Spring Boot** — Rapid application development and robust dependency injection.
- **Spring Data JPA** — For seamless ORM and database interaction.
- **PostgreSQL** — Powerful, open-source relational database.
- **REST API** — For external integrations and client interaction.
- **Redis** - For fast access to authorized user token
- **Kafka** - Communication between mic

---

## 📈 Roadmap

- [ ] Add support for Email and SMS channels
- [ ] Recipient feedback and status dashboard
- [ ] Geolocation filtering
- [ ] Admin panel or frontend UI
- [ ] Roles

---

## Architecture Diagram

![architecture schema](https://github.com/user-attachments/assets/4c39f69e-e038-4302-821a-afca3bd11b17)

### ⚡ Scalability / Low Latency

The Emergency Notification System is designed with scalability and low-latency delivery in mind, ensuring it performs efficiently even as the number of recipients or message volume increases.

- **Asynchronous Processing**:  
  Notification sending is handled asynchronously, allowing the system to quickly enqueue and dispatch messages without blocking the main thread. This ensures that even bulk messages are processed with minimal delay.

- **Optimized Bulk Operations**:  
  When importing recipients from Excel files, the system performs batch operations with optimized entity management using JPA. This minimizes database overhead and allows thousands of records to be inserted or updated efficiently.

- **Separation of Concerns**:  
  Each core operation (e.g., import, template management, message dispatch) is logically separated, making the system easier to scale horizontally by deploying components independently if needed in the future.

- **Database Indexing and Efficient Queries**:  
  Recipient records and message logs are accessed using indexed fields (e.g., email), which improves query performance during large-scale operations.

Together, these design choices ensure that the system remains responsive and scalable, capable of supporting growing usage during critical emergencies.


### Endpoints documentation

> Access all API documentation in one place using Ape-Gateway's centralized approach.
> Explore endpoints and their functionalities conveniently through
> this [link](http://localhost:8007/webjars/swagger-ui/index.html).
>
> ![authentication-service](https://github.com/user-attachments/assets/4bd7e029-d23c-46f8-93ab-ad6eb337cd3b)
> ![notificaiton-service](https://github.com/user-attachments/assets/80147790-eea5-45b1-aeea-76d4b14a7c41)
> ![template-service](https://github.com/user-attachments/assets/81c67d9f-8e10-4e35-8b2b-1c186865e74e)
> ![user-management-service](https://github.com/user-attachments/assets/4dd1073c-7b5a-4489-81d4-cb99a56bc454)

## 💬 Feedback & Contributions

Contributions are welcome! If you’d like to suggest a feature, report a bug, or help with development, feel free to open an issue or submit a pull request.

---

> ⚠️ This project is still in development. Breaking changes may occur and stability is not guaranteed for production use.
