package com.zizaizhiwu.vocabulary.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 主题-收藏夹关联（多对多），复合主键。
 */
@TableName("vocabulary_topic_collection")
public class VocabularyTopicCollection implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId
    private Long topicId;
    private Long collectionId;
    private LocalDateTime createdAt;

    public Long getTopicId() {
        return topicId;
    }

    public void setTopicId(Long topicId) {
        this.topicId = topicId;
    }

    public Long getCollectionId() {
        return collectionId;
    }

    public void setCollectionId(Long collectionId) {
        this.collectionId = collectionId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
