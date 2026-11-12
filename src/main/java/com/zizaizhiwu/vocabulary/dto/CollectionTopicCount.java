package com.zizaizhiwu.vocabulary.dto;

/**
 * 收藏夹下的主题数量统计。
 */
public class CollectionTopicCount {

    private Long collectionId;
    private Long topicCount;

    public Long getCollectionId() {
        return collectionId;
    }

    public void setCollectionId(Long collectionId) {
        this.collectionId = collectionId;
    }

    public Long getTopicCount() {
        return topicCount;
    }

    public void setTopicCount(Long topicCount) {
        this.topicCount = topicCount;
    }
}
