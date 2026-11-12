package com.zizaizhiwu.vocabulary.controller;

import com.zizaizhiwu.vocabulary.common.ApiResponse;
import com.zizaizhiwu.vocabulary.common.PageResult;
import com.zizaizhiwu.vocabulary.dto.BatchCollectionRequest;
import com.zizaizhiwu.vocabulary.dto.BatchDeleteRequest;
import com.zizaizhiwu.vocabulary.dto.BatchMoveRequest;
import com.zizaizhiwu.vocabulary.dto.CollectRequest;
import com.zizaizhiwu.vocabulary.dto.CollectionRequest;
import com.zizaizhiwu.vocabulary.dto.CollectionVO;
import com.zizaizhiwu.vocabulary.dto.LanguageCountVO;
import com.zizaizhiwu.vocabulary.dto.MoveTopicRequest;
import com.zizaizhiwu.vocabulary.dto.TopicCollectionsRequest;
import com.zizaizhiwu.vocabulary.dto.TopicDetailVO;
import com.zizaizhiwu.vocabulary.dto.TopicListRequest;
import com.zizaizhiwu.vocabulary.dto.TopicSummaryVO;
import com.zizaizhiwu.vocabulary.service.VocabularyService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

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

    /**
     * 列表顺序中的下一个主题详情，到末尾返回 404。
     */
    @GetMapping("/{id}/next")
    public ApiResponse<TopicDetailVO> next(@PathVariable Long id) {
        return ApiResponse.ok(vocabularyService.next(id));
    }

    /**
     * 列表顺序中的上一个主题详情，到开头返回 404。
     */
    @GetMapping("/{id}/prev")
    public ApiResponse<TopicDetailVO> prev(@PathVariable Long id) {
        return ApiResponse.ok(vocabularyService.prev(id));
    }

    /**
     * 把主题移动到指定收藏夹（覆盖原有全部归属）。
     */
    @PutMapping("/{id}/collection")
    public ApiResponse<TopicDetailVO> move(@PathVariable Long id,
                                           @Valid @RequestBody MoveTopicRequest request) {
        return ApiResponse.ok(vocabularyService.moveTopic(id, request.getCollectionId()));
    }

    /**
     * 把主题收藏到多个收藏夹（只增不减）。
     */
    @PostMapping("/{id}/collections")
    public ApiResponse<TopicDetailVO> addToCollections(@PathVariable Long id,
                                                       @Valid @RequestBody TopicCollectionsRequest request) {
        return ApiResponse.ok(vocabularyService.addToCollections(id, request.getCollectionIds()));
    }

    /**
     * 删除主题及其全部翻译结果。
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteTopic(@PathVariable Long id) {
        vocabularyService.deleteTopic(id);
        return ApiResponse.ok(null);
    }

    /**
     * 批量把单词收藏到多个收藏夹（只增不减）。
     */
    @PostMapping("/batch/collections")
    public ApiResponse<Void> batchAddToCollections(@Valid @RequestBody BatchCollectionRequest request) {
        vocabularyService.batchAddToCollections(request.getTopicIds(), request.getCollectionIds());
        return ApiResponse.ok(null);
    }

    /**
     * 批量移动单词到指定收藏夹（覆盖归属）。
     */
    @PostMapping("/batch/move")
    public ApiResponse<Void> batchMove(@Valid @RequestBody BatchMoveRequest request) {
        vocabularyService.batchMove(request.getTopicIds(), request.getCollectionId());
        return ApiResponse.ok(null);
    }

    /**
     * 批量删除单词及其全部翻译结果。
     */
    @PostMapping("/batch/delete")
    public ApiResponse<Void> batchDelete(@Valid @RequestBody BatchDeleteRequest request) {
        vocabularyService.batchDelete(request.getTopicIds());
        return ApiResponse.ok(null);
    }

    /**
     * 库中已有单词的语言分布，供列表页语言筛选使用。
     */
    @GetMapping("/languages")
    public ApiResponse<List<LanguageCountVO>> languages() {
        return ApiResponse.ok(vocabularyService.languageStats());
    }

    /**
     * 为存量数据补全缺失的源/目标语言，返回实际更新条数（幂等）。
     */
    @PostMapping("/languages/backfill")
    public ApiResponse<Integer> backfillLanguages() {
        return ApiResponse.ok(vocabularyService.backfillLanguages());
    }
}
