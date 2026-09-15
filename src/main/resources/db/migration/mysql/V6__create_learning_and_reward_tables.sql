CREATE TABLE user_study_reports (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 user_id BIGINT NOT NULL,
 current_streak INT NOT NULL DEFAULT 0,
 longest_streak INT NOT NULL DEFAULT 0,
 last_completion_date DATE NULL,
 streak_start_date DATE NULL,
 last_learning_timestamp DATETIME(6) NULL,
 available_freezes INT NOT NULL DEFAULT 0,
 total_reading_time_seconds BIGINT NOT NULL DEFAULT 0,
 preferred_study_hour INT NULL,
 preferred_study_hour_updated_at DATETIME(6) NULL,
 created_at DATETIME(6) NOT NULL,
 updated_at DATETIME(6) NULL,
 version BIGINT NOT NULL DEFAULT 0,
 UNIQUE KEY uq_study_reports_user (user_id),
 INDEX idx_study_reports_streak (current_streak, last_learning_timestamp),
 CONSTRAINT fk_study_reports_user FOREIGN KEY (user_id) REFERENCES users(id),
 CONSTRAINT chk_study_reports_balances CHECK (available_freezes >= 0 AND total_reading_time_seconds >= 0 AND current_streak >= 0 AND longest_streak >= 0)
) ENGINE=InnoDB;

CREATE TABLE daily_completions (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 user_id BIGINT NOT NULL,
 completion_date DATE NOT NULL,
 first_completion_count INT NOT NULL DEFAULT 0,
 total_completion_count INT NOT NULL DEFAULT 0,
 streak_count INT NULL,
 streak_status VARCHAR(20) NULL,
 created_at DATETIME(6) NOT NULL,
 UNIQUE KEY uq_daily_completions_user_date (user_id, completion_date),
 CONSTRAINT fk_daily_completions_user FOREIGN KEY (user_id) REFERENCES users(id),
 CONSTRAINT chk_daily_completion_counts CHECK (first_completion_count >= 0 AND total_completion_count >= first_completion_count)
) ENGINE=InnoDB;

CREATE TABLE learning_completions (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 daily_completion_id BIGINT NOT NULL,
 content_type VARCHAR(20) NOT NULL,
 content_id BIGINT NOT NULL,
 chapter_id BIGINT NULL,
 completed_at DATETIME(6) NOT NULL,
 reading_time INT NULL,
 category VARCHAR(255) NULL,
 difficulty_level VARCHAR(255) NULL,
 streak_status VARCHAR(20) NULL,
 INDEX idx_learning_daily_content (daily_completion_id, content_type, content_id),
 CONSTRAINT fk_learning_completion_daily FOREIGN KEY (daily_completion_id) REFERENCES daily_completions(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE freeze_transactions (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 user_id BIGINT NOT NULL,
 amount INT NOT NULL,
 description VARCHAR(500) NULL,
 effective_date DATE NULL,
 created_at DATETIME(6) NOT NULL,
 INDEX idx_freeze_user_created (user_id, created_at, id),
 INDEX idx_freeze_user_effective (user_id, effective_date),
 CONSTRAINT fk_freeze_transactions_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB;
