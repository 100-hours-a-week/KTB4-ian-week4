CREATE TABLE token_family_sessions (
    family_id VARCHAR(36) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    active_access_token_id VARCHAR(36) NOT NULL,
    revoked BOOLEAN NOT NULL
);

CREATE INDEX idx_token_family_sessions_user_id
    ON token_family_sessions (user_id);
