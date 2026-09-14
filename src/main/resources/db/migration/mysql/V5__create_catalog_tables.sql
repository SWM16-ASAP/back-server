CREATE TABLE books (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(500) NOT NULL,
    title_translations JSON NULL,
    author VARCHAR(500) NULL,
    cover_image_url VARCHAR(2048) NULL,
    difficulty_level VARCHAR(10) NOT NULL,
    chapter_count INT NULL,
    reading_time INT NULL,
    average_rating DOUBLE NOT NULL DEFAULT 0,
    review_count INT NOT NULL DEFAULT 0,
    view_count INT NOT NULL DEFAULT 0,
    tags JSON NULL,
    created_at DATETIME(6) NOT NULL,
    INDEX idx_books_created (created_at, id)
) ENGINE=InnoDB;

CREATE TABLE chapters (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    book_id BIGINT NOT NULL,
    chapter_number INT NOT NULL,
    title VARCHAR(500) NOT NULL,
    chapter_image_url VARCHAR(2048) NULL,
    description TEXT NULL,
    reading_time INT NULL,
    UNIQUE KEY uq_chapters_book_number (book_id, chapter_number),
    CONSTRAINT fk_chapters_book FOREIGN KEY (book_id) REFERENCES books (id),
    CONSTRAINT chk_chapters_number CHECK (chapter_number > 0)
) ENGINE=InnoDB;

CREATE TABLE articles (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(500) NOT NULL,
    author VARCHAR(500) NULL,
    cover_image_url VARCHAR(2048) NULL,
    origin_url VARCHAR(2048) NULL,
    difficulty_level VARCHAR(10) NOT NULL,
    reading_time INT NULL,
    average_rating DOUBLE NOT NULL DEFAULT 0,
    review_count INT NOT NULL DEFAULT 0,
    view_count INT NOT NULL DEFAULT 0,
    category VARCHAR(30) NULL,
    tags JSON NULL,
    target_language_code JSON NULL,
    created_at DATETIME(6) NOT NULL,
    INDEX idx_articles_created (created_at, id),
    INDEX idx_articles_category_created (category, created_at, id)
) ENGINE=InnoDB;
