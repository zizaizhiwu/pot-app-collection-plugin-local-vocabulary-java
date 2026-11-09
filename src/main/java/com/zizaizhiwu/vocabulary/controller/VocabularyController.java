package com.zizaizhiwu.vocabulary.controller;

import com.zizaizhiwu.vocabulary.common.ApiResponse;
import com.zizaizhiwu.vocabulary.common.PageResult;
import com.zizaizhiwu.vocabulary.dto.CollectRequest;
import com.zizaizhiwu.vocabulary.dto.TopicDetailVO;
import com.zizaizhiwu.vocabulary.dto.TopicListRequest;
import com.zizaizhiwu.vocabulary.dto.TopicSummaryVO;
import com.zizaizhiwu.vocabulary.service.VocabularyService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/topics")
@CrossOrigin(origins = "*")
public class VocabularyController {

    private final VocabularyService vocabularyService;

    public VocabularyController(VocabularyService vocabularyService) {
        this.vocabularyService = vocabularyService;
    }

    /**
     * 保存一条翻译结果（Pot 收集插件调用）。
     */
    @PostMapping
    public ApiResponse<TopicDetailVO> collect(@Valid @RequestBody CollectRequest request) {
        return ApiResponse.ok(vocabularyService.collect(request));
    }

    /**
     * 主题列表，POST 提交查询条件，分页 + 关键词搜索，按创建时间倒序。
     */
    @PostMapping("/list")
    public ApiResponse<PageResult<TopicSummaryVO>> list(
            @RequestBody(required = false) TopicListRequest request) {
        return ApiResponse.ok(vocabularyService.list(request == null ? new TopicListRequest() : request));
    }

    /**
     * 主题详情：包含该主题下所有来源的翻译结果。
     */
    @GetMapping("/{id}")
    public ApiResponse<TopicDetailVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(vocabularyService.detail(id));
    }
}
