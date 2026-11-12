package com.zizaizhiwu.vocabulary.controller;

import com.zizaizhiwu.vocabulary.common.ApiResponse;
import com.zizaizhiwu.vocabulary.dto.CollectionRequest;
import com.zizaizhiwu.vocabulary.dto.CollectionVO;
import com.zizaizhiwu.vocabulary.service.VocabularyService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/collections")
@CrossOrigin(origins = "*")
public class CollectionController {

    private final VocabularyService vocabularyService;

    public CollectionController(VocabularyService vocabularyService) {
        this.vocabularyService = vocabularyService;
    }

    /**
     * 收藏夹列表，附带主题数量；首次调用会自动创建默认收藏集。
     */
    @GetMapping
    public ApiResponse<List<CollectionVO>> list() {
        return ApiResponse.ok(vocabularyService.listCollections());
    }

    /**
     * 新建收藏夹。
     */
    @PostMapping
    public ApiResponse<CollectionVO> create(@Valid @RequestBody CollectionRequest request) {
        return ApiResponse.ok(vocabularyService.createCollection(request.getName()));
    }

    /**
     * 删除收藏夹，其中的主题移动到默认收藏集；默认收藏集不可删除。
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        vocabularyService.deleteCollection(id);
        return ApiResponse.ok(null);
    }
}
