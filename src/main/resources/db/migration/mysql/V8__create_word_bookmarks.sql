CREATE TABLE word_bookmarks (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    word VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin NOT NULL,
    bookmarked_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_word_bookmarks_user_word UNIQUE (user_id, word),
    CONSTRAINT fk_word_bookmarks_user FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_word_bookmarks_recent (user_id, bookmarked_at, id)
);
