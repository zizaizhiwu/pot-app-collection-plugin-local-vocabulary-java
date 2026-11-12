package com.zizaizhiwu.vocabulary.dto;

import java.util.List;

/**
 * 主题列表查询条件。
 */
public class TopicListRequest extends PageRequest {

    /** 原文关键词，模糊匹配，可为空 */
    private String keyword;

    /** 按收藏夹过滤（单词在该收藏夹内即可），为空表示全部 */
    private Long collectionId;

    /** 按原文语言过滤（多选），为空表示全部语言 */
    private List<String> sourceLangs;

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Long getCollectionId() {
        return collectionId;
    }

    public void setCollectionId(Long collectionId) {
        this.collectionId = collectionId;
    }

    public List<String> getSourceLangs() {
        return sourceLangs;
    }

    public void setSourceLangs(List<String> sourceLangs) {
        this.sourceLangs = sourceLangs;
    }
}
