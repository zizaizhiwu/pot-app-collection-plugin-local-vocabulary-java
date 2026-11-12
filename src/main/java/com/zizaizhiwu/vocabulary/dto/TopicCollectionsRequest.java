package com.zizaizhiwu.vocabulary.dto;

import javax.validation.constraints.NotEmpty;

import java.util.List;

/**
 * 单个单词收藏到多个收藏夹。
 */
public class TopicCollectionsRequest {

    @NotEmpty(message = "collectionIds 不能为空")
    private List<Long> collectionIds;

    public List<Long> getCollectionIds() {
        return collectionIds;
    }

    public void setCollectionIds(List<Long> collectionIds) {
        this.collectionIds = collectionIds;
    }
}
