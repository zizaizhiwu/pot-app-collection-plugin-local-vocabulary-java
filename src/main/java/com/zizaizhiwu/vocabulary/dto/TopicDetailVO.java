package com.zizaizhiwu.vocabulary.dto;

import java.util.List;

public class TopicDetailVO extends TopicSummaryVO {

    private List<ResultVO> results;

    public List<ResultVO> getResults() {
        return results;
    }

    public void setResults(List<ResultVO> results) {
        this.results = results;
    }
}
