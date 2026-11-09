package com.zizaizhiwu.vocabulary.dto;

/**
 * 主题列表查询条件。
 */
public class TopicListRequest extends PageRequest {

    /** 原文关键词，模糊匹配，可为空 */
    private String keyword;

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }
}
