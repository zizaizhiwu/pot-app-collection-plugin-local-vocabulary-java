package com.zizaizhiwu.vocabulary.dto;

/**
 * 主题所属收藏夹的简要信息（用于列表/详情展示）。
 */
public class CollectionBriefVO {

    private Long id;
    private String name;

    public CollectionBriefVO() {
    }

    public CollectionBriefVO(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
