-- 增量升级脚本：单词收藏从「单一收藏夹」改为「多收藏夹」（多对多）
-- 适用已执行过 upgrade-collections.sql 的库；执行完需要重启后端
-- 执行方式：mysql -u root -p pot_vocabulary < upgrade-topic-collections.sql

USE `pot_vocabulary`;

-- 1. 关联表
CREATE TABLE IF NOT EXISTS `vocabulary_topic_collection`
(
    `topic_id`      BIGINT UNSIGNED NOT NULL COMMENT '主题 id',
    `collection_id` BIGINT UNSIGNED NOT NULL COMMENT '收藏夹 id',
    `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
    PRIMARY KEY (`topic_id`, `collection_id`),
    KEY `idx_tc_collection` (`collection_id`),
    CONSTRAINT `fk_tc_topic` FOREIGN KEY (`topic_id`) REFERENCES `vocabulary_topic` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_tc_collection` FOREIGN KEY (`collection_id`) REFERENCES `vocabulary_collection` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='主题-收藏夹关联表';

-- 2. 把原有的单一归属迁移进关联表
INSERT IGNORE INTO `vocabulary_topic_collection` (`topic_id`, `collection_id`)
SELECT `id`, `collection_id`
FROM `vocabulary_topic`
WHERE `collection_id` IS NOT NULL;

-- 3. 删除主题表上旧的单一归属列
SET @has_fk = (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
               WHERE TABLE_SCHEMA = 'pot_vocabulary'
                 AND TABLE_NAME = 'vocabulary_topic'
                 AND CONSTRAINT_NAME = 'fk_topic_collection');
SET @ddl = IF(@has_fk > 0,
    'ALTER TABLE `vocabulary_topic` DROP FOREIGN KEY `fk_topic_collection`',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_idx = (SELECT COUNT(*) FROM information_schema.STATISTICS
                WHERE TABLE_SCHEMA = 'pot_vocabulary'
                  AND TABLE_NAME = 'vocabulary_topic'
                  AND INDEX_NAME = 'idx_collection_id');
SET @ddl = IF(@has_idx > 0,
    'ALTER TABLE `vocabulary_topic` DROP INDEX `idx_collection_id`',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_column = (SELECT COUNT(*) FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = 'pot_vocabulary'
                     AND TABLE_NAME = 'vocabulary_topic'
                     AND COLUMN_NAME = 'collection_id');
SET @ddl = IF(@has_column > 0,
    'ALTER TABLE `vocabulary_topic` DROP COLUMN `collection_id`',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
