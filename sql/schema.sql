-- 本地生词本建表脚本（MySQL 5.7+ / 8.0）
-- 执行方式：mysql -u root -p < schema.sql

CREATE DATABASE IF NOT EXISTS `pot_vocabulary`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_general_ci;

USE `pot_vocabulary`;

-- 主题表：一个「原文 + 语言对」对应一条主题，即生词本里的一行
CREATE TABLE IF NOT EXISTS `vocabulary_topic`
(
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `source_text` VARCHAR(255)    NOT NULL COMMENT '原文/被翻译的词句',
    `source_lang` VARCHAR(32)     NOT NULL DEFAULT '' COMMENT '源语言，未知为空串',
    `target_lang` VARCHAR(32)     NOT NULL DEFAULT '' COMMENT '目标语言，未知为空串',
    `created_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_topic_source_lang` (`source_text`, `source_lang`, `target_lang`),
    KEY `idx_created_at` (`created_at`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='生词本主题表';

-- 翻译结果表：同一主题下可有多条结果，每个来源（翻译插件）一条
CREATE TABLE IF NOT EXISTS `vocabulary_result`
(
    `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `topic_id`     BIGINT UNSIGNED NOT NULL COMMENT '所属主题 id',
    `provider`     VARCHAR(64)     NOT NULL DEFAULT '' COMMENT '结果来源，例如 OpenAI、DeepL、Bing',
    `content_text` MEDIUMTEXT      NOT NULL COMMENT '纯文本翻译结果',
    `content_json` MEDIUMTEXT      NULL COMMENT '结构化结果 JSON（Pot 的 explanations/pronunciations）',
    `source_lang`  VARCHAR(32)     NOT NULL DEFAULT '' COMMENT '源语言',
    `target_lang`  VARCHAR(32)     NOT NULL DEFAULT '' COMMENT '目标语言',
    `created_at`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_topic_provider` (`topic_id`, `provider`),
    KEY `idx_topic_id` (`topic_id`),
    CONSTRAINT `fk_result_topic` FOREIGN KEY (`topic_id`) REFERENCES `vocabulary_topic` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='生词本翻译结果表';
