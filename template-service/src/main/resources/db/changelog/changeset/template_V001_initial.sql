CREATE TABLE template (
    id BIGSERIAL PRIMARY KEY,
    client_username VARCHAR(255) NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT,
    CONSTRAINT template_unique_clientUsername_title UNIQUE (client_username, title)
);

CREATE TABLE username_template (
    id BIGSERIAL PRIMARY KEY,
    template_id BIGINT NOT NULL,
    username VARCHAR(255) NOT NULL,
    CONSTRAINT username_unique UNIQUE (username),
    CONSTRAINT fk_template_id FOREIGN KEY (template_id) REFERENCES template (id) ON DELETE CASCADE
);