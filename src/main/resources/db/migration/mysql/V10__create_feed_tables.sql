CREATE TABLE feeds (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    content_type VARCHAR(10) NOT NULL,
    title VARCHAR(500) NOT NULL,
    url VARCHAR(2048) NOT NULL,
    thumbnail_url VARCHAR(2048) NULL,
    author VARCHAR(500) NULL,
    description TEXT NULL,
    category VARCHAR(20) NULL,
    tags JSON NULL,
    source_provider VARCHAR(255) NULL,
    published_at DATETIME(6) NULL,
    display_order INT NULL,
    view_count INT NOT NULL DEFAULT 0,
    avg_read_time_seconds DOUBLE NULL,
    created_at DATETIME(6) NOT NULL,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at DATETIME(6) NULL,
    CONSTRAINT uk_feeds_url UNIQUE (url(255)),
    INDEX idx_feeds_category (category),
    INDEX idx_feeds_published_at (published_at)
) ENGINE=InnoDB;

CREATE TABLE feed_sources (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    url VARCHAR(2048) NOT NULL,
    domain VARCHAR(255) NULL,
    name VARCHAR(500) NOT NULL,
    cover_image_dsl TEXT NULL,
    content_type VARCHAR(10) NULL,
    category VARCHAR(20) NULL,
    tags JSON NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_feed_sources_url UNIQUE (url(255)),
    INDEX idx_feed_sources_category (category),
    INDEX idx_feed_sources_active (is_active)
) ENGINE=InnoDB;

CREATE TABLE user_category_preferences (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    primary_category VARCHAR(20) NULL,
    category_scores JSON NULL,
    raw_access_counts JSON NULL,
    total_access_count INT NULL,
    last_updated_at DATETIME(6) NULL,
    CONSTRAINT uk_user_category_preferences_user UNIQUE (user_id),
    CONSTRAINT fk_user_category_preferences_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB;
