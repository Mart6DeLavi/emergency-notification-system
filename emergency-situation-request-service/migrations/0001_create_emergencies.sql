CREATE TABLE IF NOT EXISTS emergencies (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    files JSONB,
    country VARCHAR(100),
    city VARCHAR(100),
    street VARCHAR(255),
    alarm_timestamp TIMESTAMPTZ,
    status VARCHAR(50) NOT NULL DEFAULT 'REPORTED'
        CHECK (status IN ('REPORTED', 'UNDER_REVIEW', 'CONFIRMED', 'BROADCAST', 'REJECTED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_emergencies_status ON emergencies(status);
CREATE INDEX IF NOT EXISTS idx_emergencies_user_id ON emergencies(user_id);
CREATE INDEX IF NOT EXISTS idx_emergencies_city ON emergencies(city);
