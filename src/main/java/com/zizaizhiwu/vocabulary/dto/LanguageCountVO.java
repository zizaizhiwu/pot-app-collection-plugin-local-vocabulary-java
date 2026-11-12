package com.zizaizhiwu.vocabulary.dto;

/**
 * 语言分布统计。
 */
public class LanguageCountVO {

    /** ISO 639-1 代码，未识别时为空串 */
    private String code;
    /** 中文名称 */
    private String label;
    private long count;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }
}
