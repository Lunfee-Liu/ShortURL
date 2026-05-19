CREATE TABLE `short_urls` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT,
    `short_code`   VARCHAR(8)   NOT NULL COMMENT 'generated short code, globally unique',
    `original_url` VARCHAR(2048) NOT NULL COMMENT 'original long URL',
    `shard_key`    VARCHAR(64)  DEFAULT NULL COMMENT 'reserved for future sharding',
    `is_deleted`   TINYINT(1)   NOT NULL DEFAULT 0 COMMENT 'logical deletion flag',
    `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_short_code` (`short_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='short URL mappings';
