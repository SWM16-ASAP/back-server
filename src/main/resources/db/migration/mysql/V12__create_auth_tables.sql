CREATE TABLE refresh_tokens (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    token_id VARCHAR(36) NOT NULL,
    user_id BIGINT NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_refresh_tokens_token_id UNIQUE (token_id),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_refresh_tokens_user (user_id),
    INDEX idx_refresh_tokens_expires_at (expires_at)
) ENGINE=InnoDB;

CREATE TABLE fcm_tokens (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    device_id VARCHAR(255) NOT NULL,
    fcm_token VARCHAR(500) NOT NULL,
    platform VARCHAR(10) NOT NULL,
    country_code VARCHAR(10) NULL,
    app_version VARCHAR(50) NULL,
    os_version VARCHAR(100) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_fcm_tokens_token UNIQUE (fcm_token),
    CONSTRAINT fk_fcm_tokens_user FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_fcm_tokens_user (user_id),
    INDEX idx_fcm_tokens_user_active (user_id, is_active),
    INDEX idx_fcm_tokens_updated_at (updated_at)
) ENGINE=InnoDB;
