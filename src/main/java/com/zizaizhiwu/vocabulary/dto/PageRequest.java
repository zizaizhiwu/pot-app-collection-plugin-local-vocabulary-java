package com.zizaizhiwu.vocabulary.dto;

/**
 * 分页请求基类，供各列表查询请求继承。
 */
public class PageRequest {

    /** 页码，从 1 开始 */
    private long page = 1;

    /** 每页条数，服务端上限 200 */
    private long size = 20;

    public long getPage() {
        return page;
    }

    public void setPage(long page) {
        this.page = page;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }
}
