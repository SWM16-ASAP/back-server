CREATE TABLE content_access_logs (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    content_id BIGINT NOT NULL,
    content_type VARCHAR(10) NOT NULL,
    category VARCHAR(20) NULL,
    read_time_seconds INT NULL,
    accessed_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_content_access_logs_user FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_content_access_logs_accessed_at (accessed_at)
) ENGINE=InnoDB;

CREATE TABLE push_logs (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    campaign_id VARCHAR(36) NOT NULL,
    fcm_message_id VARCHAR(255) NULL,
    campaign_group VARCHAR(255) NULL,
    user_id BIGINT NOT NULL,
    sent_at DATETIME(6) NOT NULL,
    sent_success BOOLEAN NOT NULL,
    opened_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_push_logs_campaign_id UNIQUE (campaign_id),
    CONSTRAINT fk_push_logs_user FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_push_logs_campaign_group (campaign_group),
    INDEX idx_push_logs_user (user_id),
    INDEX idx_push_logs_sent_at (sent_at),
    INDEX idx_push_logs_created_at (created_at)
) ENGINE=InnoDB;
