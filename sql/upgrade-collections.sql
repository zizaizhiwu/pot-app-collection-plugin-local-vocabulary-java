-- 增量升级脚本：为已有的生词本库新增「收藏夹」功能
-- 已按旧版 schema.sql 建过库的，执行本脚本即可，无需重跑 schema.sql
-- 执行方式：mysql -u root -p pot_vocabulary < upgrade-collections.sql

USE `pot_vocabulary`;

CREATE TABLE IF NOT EXISTS `vocabulary_collection`
(
    `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`       VARCHAR(64)     NOT NULL COMMENT '收藏夹名称',
    `is_default` TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '是否默认收藏夹，全局仅一个',
    `created_at` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_collection_name` (`name`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='生词本收藏夹表';

-- vocabulary_topic 增加 collection_id 列（已存在时跳过）
SET @has_column = (SELECT COUNT(*) FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = 'pot_vocabulary'
                     AND TABLE_NAME = 'vocabulary_topic'
                     AND COLUMN_NAME = 'collection_id');
SET @ddl = IF(@has_column = 0,
    CONCAT('ALTER TABLE `vocabulary_topic` ',
           'ADD COLUMN `collection_id` BIGINT UNSIGNED NULL COMMENT \'所属收藏夹，NULL 视为未分类\' AFTER `target_lang`, ',
           'ADD KEY `idx_collection_id` (`collection_id`), ',
           'ADD CONSTRAINT `fk_topic_collection` FOREIGN KEY (`collection_id`) REFERENCES `vocabulary_collection` (`id`) ON DELETE SET NULL'),
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 初始化默认收藏夹
INSERT INTO `vocabulary_collection` (`name`, `is_default`)
SELECT '默认收藏集', 1
WHERE NOT EXISTS(SELECT 1 FROM `vocabulary_collection` WHERE `is_default` = 1);

-- 存量主题归入默认收藏夹
UPDATE `vocabulary_topic`
SET `collection_id` = (SELECT `id` FROM `vocabulary_collection` WHERE `is_default` = 1 LIMIT 1)
WHERE `collection_id` IS NULL;
