CREATE TABLE `access_logs` (
    `id`          BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    `short_code`  VARCHAR(8)       NOT NULL COMMENT 'referenced short code',
    `ip`          VARCHAR(45)      DEFAULT NULL COMMENT 'client IP (IPv4 or IPv6)',
    `user_agent`  VARCHAR(512)     DEFAULT NULL,
    `referer`     VARCHAR(2048)    DEFAULT NULL,
    `accessed_at` DATETIME(3)      NOT NULL   COMMENT 'access timestamp with millisecond precision',
    `created_at`  DATETIME         NOT NULL   DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_short_code` (`short_code`),
    INDEX `idx_accessed_at` (`accessed_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='raw access log, append-only';
