package com.zizaizhiwu.vocabulary.dto;

import javax.validation.constraints.NotNull;

public class MoveTopicRequest {

    @NotNull(message = "collectionId 不能为空")
    private Long collectionId;

    public Long getCollectionId() {
        return collectionId;
    }

    public void setCollectionId(Long collectionId) {
        this.collectionId = collectionId;
    }
}
