-- Creating sequence for clients
CREATE SEQUENCE clients_seq START 1 INCREMENT 1;

-- Creating table clients
CREATE TABLE clients (
                         id BIGINT PRIMARY KEY DEFAULT nextval('clients_seq'),
                         name VARCHAR(50) NOT NULL,
                         second_name VARCHAR(50) NOT NULL,
                         username VARCHAR(128) UNIQUE NOT NULL,
                         password VARCHAR(255) NOT NULL,
                         email VARCHAR(255) UNIQUE NOT NULL,
                         phone_number VARCHAR(15) UNIQUE NOT NULL,
                         age INT NOT NULL CHECK (age >= 0 AND age <= 150),
                         role VARCHAR(20),
                         preferred_communication_channel VARCHAR(50),
                         creation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                         date_of_update TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                         gender VARCHAR(10),
                         blood_group VARCHAR(5) CHECK (blood_group ~ '^(A|B|AB|O)[+-]$'),
                         height NUMERIC(5, 2) CHECK (height >= 50 AND height <= 300),
                         country VARCHAR(100),
                         city VARCHAR(100),
                         CONSTRAINT unique_username_email_phone UNIQUE (username, email, phone_number)
);

-- Creating sequence for emergency_contacts
CREATE SEQUENCE emergency_contacts_seq START 1 INCREMENT 1;

-- Creating table emergency_contacts
CREATE TABLE emergency_contacts (
                                    id BIGINT PRIMARY KEY DEFAULT nextval('emergency_contacts_seq'),
                                    name VARCHAR(50) NOT NULL,
                                    second_name VARCHAR(50) NOT NULL,
                                    phone_number VARCHAR(15) NOT NULL CHECK (phone_number ~ '\+?[0-9]{10,15}'),
                                    email VARCHAR(255) UNIQUE NOT NULL CHECK (email ~ '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$'),
                                    relationship VARCHAR(50) NOT NULL,
                                    client_id BIGINT NOT NULL,
                                    CONSTRAINT fk_client_emergency FOREIGN KEY (client_id) REFERENCES clients (id) ON DELETE CASCADE
);

-- Creating sequence for system_data
CREATE SEQUENCE system_data_seq START 1 INCREMENT 1;

-- Creating table system_data
CREATE TABLE system_data (
                             id BIGINT PRIMARY KEY DEFAULT nextval('system_data_seq'),
                             ip_address VARCHAR(15) NOT NULL CHECK (ip_address ~ '^(\d{1,3}\.){3}\d{1,3}$'),
                             device VARCHAR(100) NOT NULL,
                             created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                             client_id BIGINT NOT NULL,
                             CONSTRAINT fk_client_system FOREIGN KEY (client_id) REFERENCES clients (id) ON DELETE CASCADE
);

-- Создание индексов для оптимизации запросов
CREATE INDEX idx_clients_email ON clients(email);
CREATE INDEX idx_clients_username ON clients(username);
CREATE INDEX idx_clients_phone_number ON clients(phone_number);
CREATE INDEX idx_emergency_contacts_client_id ON emergency_contacts(client_id);
CREATE INDEX idx_system_data_client_id ON system_data(client_id);
