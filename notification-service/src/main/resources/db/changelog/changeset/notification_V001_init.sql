-- 1. Удаление старых ENUM типов (если они уже существуют)
DROP TYPE IF EXISTS notification_status;
DROP TYPE IF EXISTS preferred_channel;

-- 2. Создание таблицы notification с использованием VARCHAR вместо ENUM
CREATE TABLE IF NOT EXISTS notification (
                                            id BIGSERIAL PRIMARY KEY,
                                            client_username VARCHAR(255) NOT NULL,
                                            sender_email VARCHAR(255) NOT NULL,
                                            title VARCHAR(255) NOT NULL,
                                            content TEXT,
                                            status VARCHAR(50) NOT NULL, -- Статус теперь строка
                                            preferred_channel VARCHAR(50), -- Канал теперь строка
                                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                            CONSTRAINT notification_template UNIQUE (client_username, sender_email, title)
);
