-- Fix 4: persist original_url for analytics
-- Fix 8: add is_deleted and updated_at to comply with table convention
ALTER TABLE access_logs
    ADD COLUMN original_url VARCHAR(2048)  DEFAULT NULL        COMMENT 'original long URL for analytics',
    ADD COLUMN is_deleted   TINYINT(1)     NOT NULL DEFAULT 0  COMMENT 'logical deletion flag',
    ADD COLUMN updated_at   DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;
