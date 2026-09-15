CREATE TABLE chunks (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    chapter_id BIGINT NOT NULL,
    difficulty_level VARCHAR(10) NOT NULL,
    chunk_number INT NOT NULL,
    type VARCHAR(10) NOT NULL,
    content TEXT NOT NULL,
    description TEXT NULL,
    CONSTRAINT uk_chunks_chapter_difficulty_number UNIQUE (chapter_id, difficulty_level, chunk_number),
    CONSTRAINT fk_chunks_chapter FOREIGN KEY (chapter_id) REFERENCES chapters(id) ON DELETE CASCADE,
    CONSTRAINT ck_chunks_number CHECK (chunk_number > 0)
) ENGINE=InnoDB;

CREATE TABLE article_chunks (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    article_id BIGINT NOT NULL,
    difficulty_level VARCHAR(10) NOT NULL,
    chunk_number INT NOT NULL,
    type VARCHAR(10) NOT NULL,
    content TEXT NOT NULL,
    description TEXT NULL,
    CONSTRAINT uk_article_chunks_article_difficulty_number UNIQUE (article_id, difficulty_level, chunk_number),
    CONSTRAINT fk_article_chunks_article FOREIGN KEY (article_id) REFERENCES articles(id) ON DELETE CASCADE,
    CONSTRAINT ck_article_chunks_number CHECK (chunk_number > 0)
) ENGINE=InnoDB;

CREATE TABLE custom_content_chunks (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    custom_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    difficulty_level VARCHAR(10) NOT NULL,
    chapter_num INT NOT NULL,
    chunk_num INT NOT NULL,
    type VARCHAR(10) NOT NULL,
    chunk_text TEXT NOT NULL,
    description TEXT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at DATETIME(6) NULL,
    CONSTRAINT uk_custom_content_chunks_content_difficulty_chapter_chunk UNIQUE (custom_id, difficulty_level, chapter_num, chunk_num),
    CONSTRAINT fk_custom_content_chunks_content FOREIGN KEY (custom_id) REFERENCES custom_contents(id) ON DELETE CASCADE,
    CONSTRAINT fk_custom_content_chunks_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT ck_custom_content_chunks_chapter_num CHECK (chapter_num > 0),
    CONSTRAINT ck_custom_content_chunks_chunk_num CHECK (chunk_num > 0),
    INDEX idx_custom_content_chunks_user_deleted_created (user_id, is_deleted, created_at)
) ENGINE=InnoDB;
