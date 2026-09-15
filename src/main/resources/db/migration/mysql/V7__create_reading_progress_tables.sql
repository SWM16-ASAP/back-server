CREATE TABLE book_progress (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 user_id BIGINT NOT NULL,
 book_id BIGINT NOT NULL,
 chunk_id VARCHAR(255),
 chapter_id BIGINT,
 current_read_chapter_number INT,
 max_read_chapter_number INT,
 max_read_chunk_number INT,
 normalized_progress DOUBLE,
 max_normalized_progress DOUBLE,
 current_difficulty_level VARCHAR(30),
 is_completed BOOLEAN NOT NULL DEFAULT FALSE,
 completed_at DATETIME(6),
 updated_at DATETIME(6) NOT NULL,
 version BIGINT NOT NULL DEFAULT 0,
 CONSTRAINT uk_book_progress_user_content UNIQUE (user_id, book_id),
 CONSTRAINT fk_book_progress_user FOREIGN KEY (user_id) REFERENCES users(id),
 CONSTRAINT fk_book_progress_content FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE CASCADE,
 CONSTRAINT fk_book_progress_chapter FOREIGN KEY (chapter_id) REFERENCES chapters(id) ON DELETE SET NULL,
 INDEX idx_book_progress_recent (user_id, updated_at, id)
);

CREATE TABLE article_progress (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 user_id BIGINT NOT NULL,
 article_id BIGINT NOT NULL,
 chunk_id VARCHAR(255),
 normalized_progress DOUBLE,
 max_normalized_progress DOUBLE,
 current_difficulty_level VARCHAR(30),
 is_completed BOOLEAN NOT NULL DEFAULT FALSE,
 completed_at DATETIME(6),
 updated_at DATETIME(6) NOT NULL,
 version BIGINT NOT NULL DEFAULT 0,
 CONSTRAINT uk_article_progress_user_content UNIQUE (user_id, article_id),
 CONSTRAINT fk_article_progress_user FOREIGN KEY (user_id) REFERENCES users(id),
 CONSTRAINT fk_article_progress_content FOREIGN KEY (article_id) REFERENCES articles(id) ON DELETE CASCADE,
 INDEX idx_article_progress_recent (user_id, updated_at, id)
);

CREATE TABLE custom_content_progress (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 user_id BIGINT NOT NULL,
 custom_id BIGINT NOT NULL,
 chunk_id VARCHAR(255),
 normalized_progress DOUBLE,
 max_normalized_progress DOUBLE,
 current_difficulty_level VARCHAR(30),
 is_completed BOOLEAN NOT NULL DEFAULT FALSE,
 completed_at DATETIME(6),
 updated_at DATETIME(6) NOT NULL,
 version BIGINT NOT NULL DEFAULT 0,
 CONSTRAINT uk_custom_content_progress_user_content UNIQUE (user_id, custom_id),
 CONSTRAINT fk_custom_content_progress_user FOREIGN KEY (user_id) REFERENCES users(id),
 CONSTRAINT fk_custom_content_progress_content FOREIGN KEY (custom_id) REFERENCES custom_contents(id) ON DELETE CASCADE,
 INDEX idx_custom_content_progress_recent (user_id, updated_at, id)
);

CREATE TABLE book_chapter_progress (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 book_progress_id BIGINT NOT NULL,
 chapter_number INT NOT NULL,
 progress_percentage DOUBLE,
 is_completed BOOLEAN NOT NULL DEFAULT FALSE,
 completed_at DATETIME(6),
 CONSTRAINT uk_book_chapter_progress UNIQUE (book_progress_id, chapter_number),
 CONSTRAINT fk_book_chapter_progress FOREIGN KEY (book_progress_id) REFERENCES book_progress(id) ON DELETE CASCADE,
 CONSTRAINT ck_book_chapter_number CHECK (chapter_number > 0)
);
