CREATE TABLE custom_contents (
    id BIGINT NOT NULL AUTO_INCREMENT,
    content_request_id BIGINT NOT NULL,
    creator_user_id BIGINT NOT NULL,
    title VARCHAR(500) NOT NULL,
    author VARCHAR(500) NULL,
    cover_image_url VARCHAR(2048) NULL,
    difficulty_level VARCHAR(10) NOT NULL,
    target_difficulty_levels JSON NULL,
    reading_time INT NULL,
    average_rating DOUBLE NOT NULL DEFAULT 0,
    review_count INT NOT NULL DEFAULT 0,
    view_count INT NOT NULL DEFAULT 0,
    tags JSON NULL,
    origin_url VARCHAR(2048) NULL,
    origin_domain VARCHAR(255) NULL,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_custom_contents_request (content_request_id),
    CONSTRAINT fk_custom_contents_creator FOREIGN KEY (creator_user_id) REFERENCES users (id),
    INDEX idx_custom_contents_origin_url (origin_url(255)),
    CONSTRAINT fk_custom_contents_request FOREIGN KEY (content_request_id) REFERENCES content_requests (id)
) ENGINE=InnoDB;

ALTER TABLE content_requests
    MODIFY result_custom_content_id BIGINT NULL,
    ADD CONSTRAINT fk_content_requests_result FOREIGN KEY (result_custom_content_id) REFERENCES custom_contents (id);

CREATE TABLE user_custom_contents (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    custom_content_id BIGINT NOT NULL,
    content_request_id BIGINT NOT NULL,
    unlocked_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uq_user_custom_contents_user_content (user_id, custom_content_id),
    CONSTRAINT fk_user_custom_contents_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_custom_contents_content FOREIGN KEY (custom_content_id) REFERENCES custom_contents (id),
    CONSTRAINT fk_user_custom_contents_request FOREIGN KEY (content_request_id) REFERENCES content_requests (id)
) ENGINE=InnoDB;
