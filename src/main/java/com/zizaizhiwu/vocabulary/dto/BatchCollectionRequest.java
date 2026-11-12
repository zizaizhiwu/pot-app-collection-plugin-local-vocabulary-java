package com.zizaizhiwu.vocabulary.dto;

import javax.validation.constraints.NotEmpty;

import java.util.List;

/**
 * 批量把单词收藏到多个收藏夹（只增不减）。
 */
public class BatchCollectionRequest {

    @NotEmpty(message = "topicIds 不能为空")
    private List<Long> topicIds;

    @NotEmpty(message = "collectionIds 不能为空")
    private List<Long> collectionIds;

    public List<Long> getTopicIds() {
        return topicIds;
    }

    public void setTopicIds(List<Long> topicIds) {
        this.topicIds = topicIds;
    }

    public List<Long> getCollectionIds() {
        return collectionIds;
    }

    public void setCollectionIds(List<Long> collectionIds) {
        this.collectionIds = collectionIds;
    }
}
