CREATE TABLE redis_backup(
                             id BIGINT PRIMARY KEY DEFAULT nextval('user_seq'),
                             username VARCHAR(50) NOT NULL,
                             token TEXT NOT NULL,
                             CONSTRAINT unique_username UNIQUE (username)
);