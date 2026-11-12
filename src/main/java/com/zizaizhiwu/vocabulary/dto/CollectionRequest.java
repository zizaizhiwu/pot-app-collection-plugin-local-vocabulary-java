package com.zizaizhiwu.vocabulary.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class CollectionRequest {

    @NotBlank(message = "收藏夹名称不能为空")
    @Size(max = 64, message = "收藏夹名称不能超过 64 个字符")
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
