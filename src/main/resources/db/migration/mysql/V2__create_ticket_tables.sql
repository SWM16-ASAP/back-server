CREATE TABLE ticket_wallets (
    user_id BIGINT NOT NULL,
    balance INT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (user_id),
    CONSTRAINT fk_ticket_wallets_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT chk_ticket_wallets_balance CHECK (balance >= 0)
) ENGINE=InnoDB;

CREATE TABLE ticket_reservations (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    amount INT NOT NULL,
    description VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    INDEX idx_ticket_reservations_user_status (user_id, status),
    CONSTRAINT fk_ticket_reservations_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT chk_ticket_reservations_amount CHECK (amount > 0)
) ENGINE=InnoDB;

CREATE TABLE ticket_transactions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    amount INT NOT NULL,
    description VARCHAR(500) NOT NULL,
    reservation_id BIGINT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uq_ticket_transactions_reservation (reservation_id),
    INDEX idx_ticket_transactions_user_created_at (user_id, created_at DESC),
    CONSTRAINT fk_ticket_transactions_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_ticket_transactions_reservation FOREIGN KEY (reservation_id) REFERENCES ticket_reservations (id)
) ENGINE=InnoDB;
