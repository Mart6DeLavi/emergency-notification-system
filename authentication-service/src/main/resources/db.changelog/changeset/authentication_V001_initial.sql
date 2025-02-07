CREATE SEQUENCE user_seq START 1 INCREMENT 1;

CREATE TABLE authentication_user(
    id BIGINT PRIMARY KEY DEFAULT nextval('user_seq'),
    username VARCHAR(50) NOT NULL,
    password VARCHAR(255) NOT NULL,
    CONSTRAINT unique_username UNIQUE (username)
);