CREATE TABLE crawling_dsl (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    domain VARCHAR(255) NOT NULL,
    name VARCHAR(500) NOT NULL,
    content_type VARCHAR(10) NULL,
    title_dsl TEXT NOT NULL,
    content_dsl TEXT NOT NULL,
    cover_image_dsl TEXT NULL,
    access_url VARCHAR(2048) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_crawling_dsl_domain UNIQUE (domain)
) ENGINE=InnoDB;

CREATE TABLE content_banners (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    country_code VARCHAR(10) NOT NULL,
    content_id BIGINT NOT NULL,
    content_type VARCHAR(10) NOT NULL,
    content_title VARCHAR(500) NULL,
    content_author VARCHAR(500) NULL,
    content_cover_image_url VARCHAR(2048) NULL,
    content_reading_time INT NULL,
    subtitle VARCHAR(500) NULL,
    title VARCHAR(500) NOT NULL,
    description TEXT NOT NULL,
    display_order INT NOT NULL DEFAULT 9,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL,
    INDEX idx_content_banners_country_active_order (country_code, is_active, display_order),
    INDEX idx_content_banners_country_order (country_code, display_order)
) ENGINE=InnoDB;

CREATE TABLE app_version (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    latest_version VARCHAR(50) NOT NULL,
    minimum_version VARCHAR(50) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    INDEX idx_app_version_updated (updated_at)
) ENGINE=InnoDB;
