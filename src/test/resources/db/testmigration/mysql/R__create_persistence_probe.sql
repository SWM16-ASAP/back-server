CREATE TABLE persistence_probe (
    id BIGINT NOT NULL PRIMARY KEY,
    value_text VARCHAR(100) NOT NULL,
    CONSTRAINT uq_probe_value UNIQUE (value_text)
) ENGINE=InnoDB;
