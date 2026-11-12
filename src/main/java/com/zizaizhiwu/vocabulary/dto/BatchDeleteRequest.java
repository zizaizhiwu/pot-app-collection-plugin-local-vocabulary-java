package com.zizaizhiwu.vocabulary.dto;

import javax.validation.constraints.NotEmpty;

import java.util.List;

/**
 * 批量删除单词。
 */
public class BatchDeleteRequest {

    @NotEmpty(message = "topicIds 不能为空")
    private List<Long> topicIds;

    public List<Long> getTopicIds() {
        return topicIds;
    }

    public void setTopicIds(List<Long> topicIds) {
        this.topicIds = topicIds;
    }
}
