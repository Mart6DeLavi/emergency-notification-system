CREATE TABLE files (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL,
    url VARCHAR(1024),
    filename VARCHAR(512) NOT NULL,
    emergency_situation_id BIGINT,
    moderation_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_files_user_id ON files(user_id);
CREATE INDEX idx_files_emergency_situation_id ON files(emergency_situation_id);
