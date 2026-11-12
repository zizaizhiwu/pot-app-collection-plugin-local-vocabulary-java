package com.zizaizhiwu.vocabulary.dto;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import java.util.List;

/**
 * 批量移动单词到指定收藏夹（移动 = 覆盖原有全部收藏夹归属）。
 */
public class BatchMoveRequest {

    @NotEmpty(message = "topicIds 不能为空")
    private List<Long> topicIds;

    @NotNull(message = "collectionId 不能为空")
    private Long collectionId;

    public List<Long> getTopicIds() {
        return topicIds;
    }

    public void setTopicIds(List<Long> topicIds) {
        this.topicIds = topicIds;
    }

    public Long getCollectionId() {
        return collectionId;
    }

    public void setCollectionId(Long collectionId) {
        this.collectionId = collectionId;
    }
}
