DROP TABLE IF EXISTS username_template CASCADE;
DROP TABLE IF EXISTS template CASCADE;

CREATE TABLE IF NOT EXISTS templates (
    id BIGSERIAL PRIMARY KEY,
    template_name VARCHAR(255) NOT NULL UNIQUE,
    description VARCHAR(500),
    channel VARCHAR(10) NOT NULL CHECK (channel IN ('EMAIL', 'PUSH', 'SMS')),
    content TEXT NOT NULL,
    created_by UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_templates_channel ON templates(channel);
CREATE INDEX idx_templates_created_by ON templates(created_by);
