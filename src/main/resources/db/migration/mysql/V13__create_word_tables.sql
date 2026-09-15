CREATE TABLE words (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    word VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin NOT NULL,
    source_language_code VARCHAR(10) NOT NULL,
    target_language_code VARCHAR(10) NOT NULL,
    summary JSON NULL,
    meanings JSON NULL,
    related_forms JSON NULL,
    is_essential BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_words_word_target_source UNIQUE (word, target_language_code, source_language_code),
    INDEX idx_words_essential_target (is_essential, target_language_code)
) ENGINE=InnoDB;

CREATE TABLE word_variants (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    word VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin NOT NULL,
    original_form VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin NOT NULL,
    variant_types JSON NULL,
    CONSTRAINT uk_word_variants_word_original UNIQUE (word, original_form),
    INDEX idx_word_variants_original_form (original_form)
) ENGINE=InnoDB;

CREATE TABLE invalid_words (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    word VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin NOT NULL,
    attempted_at DATETIME(6) NOT NULL,
    attempt_count INT NOT NULL DEFAULT 1,
    CONSTRAINT uk_invalid_words_word UNIQUE (word)
) ENGINE=InnoDB;
