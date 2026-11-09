package com.zizaizhiwu.vocabulary.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * Pot 收集插件提交的数据。
 */
public class CollectRequest {

    @NotBlank(message = "sourceText 不能为空")
    @Size(max = 255, message = "sourceText 长度不能超过 255")
    private String sourceText;

    @Size(max = 32, message = "sourceLang 长度不能超过 32")
    private String sourceLang;

    @Size(max = 32, message = "targetLang 长度不能超过 32")
    private String targetLang;

    @Size(max = 64, message = "provider 长度不能超过 64")
    private String provider;

    @NotBlank(message = "contentText 不能为空")
    private String contentText;

    private String contentJson;

    public String getSourceText() {
        return sourceText;
    }

    public void setSourceText(String sourceText) {
        this.sourceText = sourceText;
    }

    public String getSourceLang() {
        return sourceLang;
    }

    public void setSourceLang(String sourceLang) {
        this.sourceLang = sourceLang;
    }

    public String getTargetLang() {
        return targetLang;
    }

    public void setTargetLang(String targetLang) {
        this.targetLang = targetLang;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getContentText() {
        return contentText;
    }

    public void setContentText(String contentText) {
        this.contentText = contentText;
    }

    public String getContentJson() {
        return contentJson;
    }

    public void setContentJson(String contentJson) {
        this.contentJson = contentJson;
    }
}
