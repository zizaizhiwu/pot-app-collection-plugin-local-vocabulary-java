package com.zizaizhiwu.vocabulary.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zizaizhiwu.vocabulary.common.BusinessException;
import com.zizaizhiwu.vocabulary.common.PageResult;
import com.zizaizhiwu.vocabulary.dto.CollectRequest;
import com.zizaizhiwu.vocabulary.dto.CollectionBriefVO;
import com.zizaizhiwu.vocabulary.dto.CollectionTopicCount;
import com.zizaizhiwu.vocabulary.dto.CollectionVO;
import com.zizaizhiwu.vocabulary.dto.LanguageCountVO;
import com.zizaizhiwu.vocabulary.dto.ResultVO;
import com.zizaizhiwu.vocabulary.dto.TopicDetailVO;
import com.zizaizhiwu.vocabulary.dto.TopicListRequest;
import com.zizaizhiwu.vocabulary.dto.TopicResultCount;
import com.zizaizhiwu.vocabulary.dto.TopicSummaryVO;
import com.zizaizhiwu.vocabulary.entity.VocabularyCollection;
import com.zizaizhiwu.vocabulary.entity.VocabularyResult;
import com.zizaizhiwu.vocabulary.entity.VocabularyTopic;
import com.zizaizhiwu.vocabulary.entity.VocabularyTopicCollection;
import com.zizaizhiwu.vocabulary.mapper.VocabularyCollectionMapper;
import com.zizaizhiwu.vocabulary.mapper.VocabularyResultMapper;
import com.zizaizhiwu.vocabulary.mapper.VocabularyTopicCollectionMapper;
import com.zizaizhiwu.vocabulary.mapper.VocabularyTopicMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class VocabularyService {

    public static final String DEFAULT_COLLECTION_NAME = "默认收藏集";

    private final VocabularyTopicMapper topicMapper;
    private final VocabularyResultMapper resultMapper;
    private final VocabularyCollectionMapper collectionMapper;
    private final VocabularyTopicCollectionMapper topicCollectionMapper;
    private final LanguageDetectionService languageDetectionService;

    public VocabularyService(VocabularyTopicMapper topicMapper,
                             VocabularyResultMapper resultMapper,
                             VocabularyCollectionMapper collectionMapper,
                             VocabularyTopicCollectionMapper topicCollectionMapper,
                             LanguageDetectionService languageDetectionService) {
        this.topicMapper = topicMapper;
        this.resultMapper = resultMapper;
        this.collectionMapper = collectionMapper;
        this.topicCollectionMapper = topicCollectionMapper;
        this.languageDetectionService = languageDetectionService;
    }

    /**
     * 保存一条翻译结果：主题按「原文 + 源语言 + 目标语言」去重，
     * 同一主题下每个 provider 只保留一条结果，重复提交视为更新。
     */
    @Transactional
    public TopicDetailVO collect(CollectRequest request) {
        String sourceText = request.getSourceText().trim();
        // 采集端拿不到翻译方向，缺失时由服务端根据文本识别
        String sourceLang = normalize(request.getSourceLang());
        if (sourceLang.isEmpty()) {
            sourceLang = languageDetectionService.detectSource(sourceText);
        }
        String targetLang = normalize(request.getTargetLang());
        if (targetLang.isEmpty()) {
            targetLang = languageDetectionService.detect(request.getContentText());
        }
        // 采集端目前拿不到翻译服务名，provider 允许为空（字段保留，便于将来支持区分来源）
        String provider = normalize(request.getProvider());
        String contentJson = request.getContentJson() == null ? "" : request.getContentJson();

        LocalDateTime now = LocalDateTime.now();

        VocabularyTopic topic = findTopic(sourceText, sourceLang, targetLang);
        if (topic == null) {
            topic = new VocabularyTopic();
            topic.setSourceText(sourceText);
            topic.setSourceLang(sourceLang);
            topic.setTargetLang(targetLang);
            topic.setCreatedAt(now);
            topic.setUpdatedAt(now);
            try {
                topicMapper.insert(topic);
            } catch (DuplicateKeyException e) {
                // 并发提交同一主题时，回查已存在的记录
                topic = findTopic(sourceText, sourceLang, targetLang);
                if (topic == null) {
                    throw e;
                }
            }
            // 新收藏的单词进入默认收藏夹；已存在的主题保持原有收藏夹归属不变
            linkCollection(topic.getId(), ensureDefaultCollection());
        } else {
            // 老数据可能缺语言信息，重新收藏时顺带补全
            if (normalize(topic.getSourceLang()).isEmpty() && !sourceLang.isEmpty()) {
                topic.setSourceLang(sourceLang);
            }
            if (normalize(topic.getTargetLang()).isEmpty() && !targetLang.isEmpty()) {
                topic.setTargetLang(targetLang);
            }
            topic.setUpdatedAt(now);
            topicMapper.updateById(topic);
        }

        VocabularyResult result = findResult(topic.getId(), provider);
        if (result == null) {
            result = new VocabularyResult();
            result.setTopicId(topic.getId());
            result.setProvider(provider);
            result.setContentText(request.getContentText());
            result.setContentJson(contentJson);
            result.setSourceLang(sourceLang);
            result.setTargetLang(targetLang);
            result.setCreatedAt(now);
            result.setUpdatedAt(now);
            try {
                resultMapper.insert(result);
            } catch (DuplicateKeyException e) {
                result = findResult(topic.getId(), provider);
                if (result == null) {
                    throw e;
                }
                result.setContentText(request.getContentText());
                result.setContentJson(contentJson);
                result.setUpdatedAt(now);
                resultMapper.updateById(result);
            }
        } else {
            result.setContentText(request.getContentText());
            result.setContentJson(contentJson);
            result.setSourceLang(sourceLang);
            result.setTargetLang(targetLang);
            result.setUpdatedAt(now);
            resultMapper.updateById(result);
        }

        return detail(topic.getId());
    }

    public PageResult<TopicSummaryVO> list(TopicListRequest request) {
        long currentPage = request.getPage() < 1 ? 1 : request.getPage();
        long pageSize = request.getSize() < 1 ? 20 : Math.min(request.getSize(), 100);
        String keyword = request.getKeyword();

        LambdaQueryWrapper<VocabularyTopic> wrapper = new LambdaQueryWrapper<>();
        if (request.getCollectionId() != null) {
            // 多对多：取关联表中该收藏夹下的全部主题 id
            List<Long> topicIds = topicCollectionMapper.selectTopicIdsByCollectionId(request.getCollectionId());
            if (topicIds.isEmpty()) {
                return new PageResult<>(0, currentPage, pageSize, Collections.emptyList());
            }
            wrapper.in(VocabularyTopic::getId, topicIds);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.like(VocabularyTopic::getSourceText, keyword.trim());
        }
        if (request.getSourceLangs() != null && !request.getSourceLangs().isEmpty()) {
            wrapper.in(VocabularyTopic::getSourceLang, request.getSourceLangs());
        }
        wrapper.orderByDesc(VocabularyTopic::getCreatedAt).orderByDesc(VocabularyTopic::getId);

        Page<VocabularyTopic> pageParam = new Page<>(currentPage, pageSize);
        Page<VocabularyTopic> result = topicMapper.selectPage(pageParam, wrapper);

        List<VocabularyTopic> topics = result.getRecords();
        Map<Long, Long> countMap = countResults(topics);
        Map<Long, List<CollectionBriefVO>> collectionsMap = loadTopicCollections(topics);

        List<TopicSummaryVO> records = new ArrayList<>(topics.size());
        for (VocabularyTopic topic : topics) {
            TopicSummaryVO vo = new TopicSummaryVO();
            fillSummary(vo, topic, collectionsMap.get(topic.getId()));
            Long count = countMap.get(topic.getId());
            vo.setResultCount(count == null ? 0 : count);
            records.add(vo);
        }
        return new PageResult<>(result.getTotal(), result.getCurrent(), result.getSize(), records);
    }

    public TopicDetailVO detail(Long id) {
        VocabularyTopic topic = topicMapper.selectById(id);
        if (topic == null) {
            throw new BusinessException(404, "主题不存在: " + id);
        }

        TopicDetailVO vo = new TopicDetailVO();
        fillSummary(vo, topic, loadTopicCollections(Collections.singletonList(topic)).get(topic.getId()));

        List<VocabularyResult> results = resultMapper.selectList(
                new LambdaQueryWrapper<VocabularyResult>()
                        .eq(VocabularyResult::getTopicId, topic.getId())
                        .orderByAsc(VocabularyResult::getId));

        List<ResultVO> resultVOs = new ArrayList<>(results.size());
        for (VocabularyResult item : results) {
            ResultVO resultVO = new ResultVO();
            resultVO.setId(item.getId());
            resultVO.setProvider(item.getProvider());
            resultVO.setContentText(item.getContentText());
            resultVO.setContentJson(item.getContentJson());
            resultVO.setCreatedAt(item.getCreatedAt());
            resultVO.setUpdatedAt(item.getUpdatedAt());
            resultVOs.add(resultVO);
        }
        vo.setResults(resultVOs);
        vo.setResultCount(resultVOs.size());
        return vo;
    }

    /**
     * 列表顺序（created_at desc, id desc）中的下一个主题详情。
     */
    public TopicDetailVO next(Long id) {
        return neighbor(id, true);
    }

    /**
     * 列表顺序中的上一个主题详情。
     */
    public TopicDetailVO prev(Long id) {
        return neighbor(id, false);
    }

    private TopicDetailVO neighbor(Long id, boolean forward) {
        VocabularyTopic current = topicMapper.selectById(id);
        if (current == null) {
            throw new BusinessException(404, "主题不存在: " + id);
        }

        LambdaQueryWrapper<VocabularyTopic> wrapper = new LambdaQueryWrapper<>();
        if (forward) {
            wrapper.lt(VocabularyTopic::getCreatedAt, current.getCreatedAt())
                    .or(nested -> nested.eq(VocabularyTopic::getCreatedAt, current.getCreatedAt())
                            .lt(VocabularyTopic::getId, current.getId()))
                    .orderByDesc(VocabularyTopic::getCreatedAt)
                    .orderByDesc(VocabularyTopic::getId);
        } else {
            wrapper.gt(VocabularyTopic::getCreatedAt, current.getCreatedAt())
                    .or(nested -> nested.eq(VocabularyTopic::getCreatedAt, current.getCreatedAt())
                            .gt(VocabularyTopic::getId, current.getId()))
                    .orderByAsc(VocabularyTopic::getCreatedAt)
                    .orderByAsc(VocabularyTopic::getId);
        }

        VocabularyTopic neighborTopic = topicMapper.selectOne(wrapper.last("limit 1"));
        if (neighborTopic == null) {
            throw new BusinessException(404, forward ? "已经是最后一个" : "已经是第一个");
        }
        return detail(neighborTopic.getId());
    }

    /**
     * 收藏夹列表，附带每个收藏夹的主题数量（按关联表统计）。
     */
    @Transactional
    public List<CollectionVO> listCollections() {
        ensureDefaultCollection();

        Map<Long, Long> countMap = new HashMap<>();
        for (CollectionTopicCount row : topicCollectionMapper.countTopicsByCollection()) {
            countMap.put(row.getCollectionId(), row.getTopicCount());
        }

        List<VocabularyCollection> all = collectionMapper.selectList(
                new LambdaQueryWrapper<VocabularyCollection>()
                        .orderByDesc(VocabularyCollection::getIsDefault)
                        .orderByAsc(VocabularyCollection::getId));

        List<CollectionVO> result = new ArrayList<>(all.size());
        for (VocabularyCollection item : all) {
            CollectionVO vo = new CollectionVO();
            vo.setId(item.getId());
            vo.setName(item.getName());
            vo.setIsDefault(Boolean.TRUE.equals(item.getIsDefault()));
            Long count = countMap.get(item.getId());
            vo.setTopicCount(count == null ? 0 : count);
            result.add(vo);
        }
        return result;
    }

    /**
     * 数据库中已有单词的语言分布（按原文语言），用于列表页快速筛选。
     */
    public List<LanguageCountVO> languageStats() {
        List<LanguageCountVO> rows = topicMapper.countBySourceLang();
        for (LanguageCountVO row : rows) {
            row.setLabel(languageDetectionService.label(row.getCode()));
        }
        return rows;
    }

    /**
     * 为存量数据补全缺失的源/目标语言，返回实际更新的条数。
     */
    @Transactional
    public int backfillLanguages() {
        List<VocabularyTopic> topics = topicMapper.selectList(null);
        if (topics.isEmpty()) {
            return 0;
        }

        List<Long> topicIds = topics.stream().map(VocabularyTopic::getId).collect(Collectors.toList());
        List<VocabularyResult> results = resultMapper.selectList(
                new LambdaQueryWrapper<VocabularyResult>()
                        .in(VocabularyResult::getTopicId, topicIds));
        Map<Long, List<VocabularyResult>> resultsByTopic = results.stream()
                .collect(Collectors.groupingBy(VocabularyResult::getTopicId));

        int changed = 0;
        for (VocabularyTopic topic : topics) {
            boolean needUpdate = false;

            if (normalize(topic.getSourceLang()).isEmpty()) {
                String detected = languageDetectionService.detectSource(topic.getSourceText());
                if (!detected.isEmpty()) {
                    topic.setSourceLang(detected);
                    needUpdate = true;
                }
            }

            if (normalize(topic.getTargetLang()).isEmpty()) {
                List<VocabularyResult> topicResults = resultsByTopic.get(topic.getId());
                if (topicResults != null && !topicResults.isEmpty()) {
                    StringBuilder text = new StringBuilder();
                    for (VocabularyResult item : topicResults) {
                        text.append(' ').append(item.getContentText() == null ? "" : item.getContentText());
                    }
                    String detected = languageDetectionService.detect(text.toString());
                    if (!detected.isEmpty()) {
                        topic.setTargetLang(detected);
                        needUpdate = true;
                    }
                }
            }

            if (needUpdate) {
                topicMapper.updateById(topic);
                changed++;
            }
        }
        return changed;
    }

    @Transactional
    public CollectionVO createCollection(String name) {
        String trimmed = normalize(name);
        if (trimmed.isEmpty()) {
            throw new BusinessException(400, "收藏夹名称不能为空");
        }
        if (trimmed.length() > 64) {
            throw new BusinessException(400, "收藏夹名称不能超过 64 个字符");
        }
        Long count = collectionMapper.selectCount(new LambdaQueryWrapper<VocabularyCollection>()
                .eq(VocabularyCollection::getName, trimmed));
        if (count != null && count > 0) {
            throw new BusinessException(400, "收藏夹「" + trimmed + "」已存在");
        }

        VocabularyCollection collection = new VocabularyCollection();
        collection.setName(trimmed);
        collection.setIsDefault(false);
        collection.setCreatedAt(LocalDateTime.now());
        collectionMapper.insert(collection);

        CollectionVO vo = new CollectionVO();
        vo.setId(collection.getId());
        vo.setName(collection.getName());
        vo.setIsDefault(false);
        vo.setTopicCount(0);
        return vo;
    }

    /**
     * 删除收藏夹；默认收藏夹不可删除。单词在其它收藏夹中的归属不受影响。
     */
    @Transactional
    public void deleteCollection(Long id) {
        VocabularyCollection collection = collectionMapper.selectById(id);
        if (collection == null) {
            throw new BusinessException(404, "收藏夹不存在: " + id);
        }
        if (Boolean.TRUE.equals(collection.getIsDefault())) {
            throw new BusinessException(400, "默认收藏集不能删除");
        }
        // 关联表有 ON DELETE CASCADE 外键，直接删收藏夹即可
        collectionMapper.deleteById(id);
    }

    /**
     * 把主题移动到指定收藏夹：覆盖该主题的全部收藏夹归属，只保留目标收藏夹。
     */
    @Transactional
    public TopicDetailVO moveTopic(Long topicId, Long collectionId) {
        checkTopicAndCollection(topicId, collectionId);
        topicCollectionMapper.delete(new LambdaQueryWrapper<VocabularyTopicCollection>()
                .eq(VocabularyTopicCollection::getTopicId, topicId));
        linkCollection(topicId, collectionId);
        return detail(topicId);
    }

    /**
     * 单个单词收藏到多个收藏夹（只增不减）。
     */
    @Transactional
    public TopicDetailVO addToCollections(Long topicId, List<Long> collectionIds) {
        validateCollectionIds(collectionIds);
        for (Long collectionId : collectionIds) {
            linkCollection(topicId, collectionId);
        }
        if (topicMapper.selectById(topicId) == null) {
            throw new BusinessException(404, "主题不存在: " + topicId);
        }
        return detail(topicId);
    }

    /**
     * 批量把单词收藏到多个收藏夹（只增不减）。
     */
    @Transactional
    public void batchAddToCollections(List<Long> topicIds, List<Long> collectionIds) {
        validateCollectionIds(collectionIds);
        for (Long topicId : topicIds) {
            for (Long collectionId : collectionIds) {
                linkCollection(topicId, collectionId);
            }
        }
    }

    /**
     * 批量移动单词到指定收藏夹（覆盖归属）。
     */
    @Transactional
    public void batchMove(List<Long> topicIds, Long collectionId) {
        checkCollectionExists(collectionId);
        topicCollectionMapper.delete(new LambdaQueryWrapper<VocabularyTopicCollection>()
                .in(VocabularyTopicCollection::getTopicId, topicIds));
        for (Long topicId : topicIds) {
            if (topicMapper.selectById(topicId) == null) {
                continue;
            }
            linkCollection(topicId, collectionId);
        }
    }

    /**
     * 批量删除单词及其全部翻译结果。
     */
    @Transactional
    public void batchDelete(List<Long> topicIds) {
        resultMapper.delete(new LambdaQueryWrapper<VocabularyResult>()
                .in(VocabularyResult::getTopicId, topicIds));
        topicMapper.delete(new LambdaQueryWrapper<VocabularyTopic>()
                .in(VocabularyTopic::getId, topicIds));
    }

    /**
     * 删除主题及其全部翻译结果。
     */
    @Transactional
    public void deleteTopic(Long topicId) {
        VocabularyTopic topic = topicMapper.selectById(topicId);
        if (topic == null) {
            throw new BusinessException(404, "主题不存在: " + topicId);
        }
        resultMapper.delete(new LambdaQueryWrapper<VocabularyResult>()
                .eq(VocabularyResult::getTopicId, topicId));
        topicMapper.deleteById(topicId);
    }

    private void checkTopicAndCollection(Long topicId, Long collectionId) {
        if (topicMapper.selectById(topicId) == null) {
            throw new BusinessException(404, "主题不存在: " + topicId);
        }
        checkCollectionExists(collectionId);
    }

    private void checkCollectionExists(Long collectionId) {
        if (collectionId == null || collectionMapper.selectById(collectionId) == null) {
            throw new BusinessException(404, "收藏夹不存在: " + collectionId);
        }
    }

    private void validateCollectionIds(List<Long> collectionIds) {
        if (collectionIds == null || collectionIds.isEmpty()) {
            throw new BusinessException(400, "collectionIds 不能为空");
        }
        for (Long collectionId : collectionIds) {
            checkCollectionExists(collectionId);
        }
    }

    /**
     * 建立「主题-收藏夹」关联，已存在时忽略（幂等）。
     */
    private void linkCollection(Long topicId, Long collectionId) {
        if (topicId == null || collectionId == null) {
            return;
        }
        Long exists = topicCollectionMapper.selectCount(
                new LambdaQueryWrapper<VocabularyTopicCollection>()
                        .eq(VocabularyTopicCollection::getTopicId, topicId)
                        .eq(VocabularyTopicCollection::getCollectionId, collectionId));
        if (exists != null && exists > 0) {
            return;
        }
        VocabularyTopicCollection link = new VocabularyTopicCollection();
        link.setTopicId(topicId);
        link.setCollectionId(collectionId);
        link.setCreatedAt(LocalDateTime.now());
        try {
            topicCollectionMapper.insert(link);
        } catch (DuplicateKeyException e) {
            // 并发下重复插入，视为已收藏
        }
    }

    /**
     * 确保默认收藏夹存在，返回其 id。
     */
    private Long ensureDefaultCollection() {
        VocabularyCollection existing = collectionMapper.selectOne(
                new LambdaQueryWrapper<VocabularyCollection>()
                        .eq(VocabularyCollection::getName, DEFAULT_COLLECTION_NAME)
                        .last("limit 1"));
        if (existing != null) {
            if (!Boolean.TRUE.equals(existing.getIsDefault())) {
                existing.setIsDefault(true);
                collectionMapper.updateById(existing);
            }
            return existing.getId();
        }

        VocabularyCollection created = new VocabularyCollection();
        created.setName(DEFAULT_COLLECTION_NAME);
        created.setIsDefault(true);
        created.setCreatedAt(LocalDateTime.now());
        try {
            collectionMapper.insert(created);
        } catch (DuplicateKeyException e) {
            VocabularyCollection fallback = collectionMapper.selectOne(
                    new LambdaQueryWrapper<VocabularyCollection>()
                            .eq(VocabularyCollection::getName, DEFAULT_COLLECTION_NAME)
                            .last("limit 1"));
            if (fallback == null) {
                throw e;
            }
            return fallback.getId();
        }
        return created.getId();
    }

    private void fillSummary(TopicSummaryVO vo, VocabularyTopic topic, List<CollectionBriefVO> collections) {
        vo.setId(topic.getId());
        vo.setSourceText(topic.getSourceText());
        vo.setSourceLang(topic.getSourceLang());
        vo.setTargetLang(topic.getTargetLang());
        vo.setCollections(collections == null ? Collections.emptyList() : collections);
        vo.setCreatedAt(topic.getCreatedAt());
        vo.setUpdatedAt(topic.getUpdatedAt());
    }

    /**
     * 批量查询单词的收藏夹归属，key 为 topicId。
     */
    private Map<Long, List<CollectionBriefVO>> loadTopicCollections(List<VocabularyTopic> topics) {
        if (topics.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> topicIds = topics.stream().map(VocabularyTopic::getId).collect(Collectors.toList());
        List<VocabularyTopicCollection> links = topicCollectionMapper.selectByTopicIds(topicIds);

        Map<Long, String> names = new HashMap<>();
        for (VocabularyCollection item : collectionMapper.selectList(null)) {
            names.put(item.getId(), item.getName());
        }

        Map<Long, List<CollectionBriefVO>> result = new HashMap<>();
        for (VocabularyTopicCollection link : links) {
            String name = names.get(link.getCollectionId());
            result.computeIfAbsent(link.getTopicId(), k -> new ArrayList<>())
                    .add(new CollectionBriefVO(link.getCollectionId(), name));
        }
        return result;
    }

    private Map<Long, Long> countResults(List<VocabularyTopic> topics) {
        if (topics.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> ids = topics.stream().map(VocabularyTopic::getId).collect(Collectors.toList());
        List<TopicResultCount> rows = resultMapper.countByTopicIds(ids);
        Map<Long, Long> countMap = new HashMap<>(rows.size());
        for (TopicResultCount row : rows) {
            countMap.put(row.getTopicId(), row.getResultCount());
        }
        return countMap;
    }

    private VocabularyTopic findTopic(String sourceText, String sourceLang, String targetLang) {
        return topicMapper.selectOne(new LambdaQueryWrapper<VocabularyTopic>()
                .eq(VocabularyTopic::getSourceText, sourceText)
                .eq(VocabularyTopic::getSourceLang, sourceLang)
                .eq(VocabularyTopic::getTargetLang, targetLang)
                .last("limit 1"));
    }

    private VocabularyResult findResult(Long topicId, String provider) {
        LambdaQueryWrapper<VocabularyResult> wrapper = new LambdaQueryWrapper<VocabularyResult>()
                .eq(VocabularyResult::getTopicId, topicId);
        if (!provider.isEmpty()) {
            wrapper.eq(VocabularyResult::getProvider, provider);
        }
        // provider 为空时视为「该主题只有一条结果」，取最早的一条复用，避免重复收藏产生多行
        wrapper.orderByAsc(VocabularyResult::getId).last("limit 1");
        return resultMapper.selectOne(wrapper);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
