package com.zizaizhiwu.vocabulary.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zizaizhiwu.vocabulary.common.BusinessException;
import com.zizaizhiwu.vocabulary.common.PageResult;
import com.zizaizhiwu.vocabulary.dto.CollectRequest;
import com.zizaizhiwu.vocabulary.dto.ResultVO;
import com.zizaizhiwu.vocabulary.dto.TopicDetailVO;
import com.zizaizhiwu.vocabulary.dto.TopicListRequest;
import com.zizaizhiwu.vocabulary.dto.TopicResultCount;
import com.zizaizhiwu.vocabulary.dto.TopicSummaryVO;
import com.zizaizhiwu.vocabulary.entity.VocabularyResult;
import com.zizaizhiwu.vocabulary.entity.VocabularyTopic;
import com.zizaizhiwu.vocabulary.mapper.VocabularyResultMapper;
import com.zizaizhiwu.vocabulary.mapper.VocabularyTopicMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class VocabularyService {

    private static final String DEFAULT_PROVIDER = "unknown";

    private final VocabularyTopicMapper topicMapper;
    private final VocabularyResultMapper resultMapper;

    public VocabularyService(VocabularyTopicMapper topicMapper, VocabularyResultMapper resultMapper) {
        this.topicMapper = topicMapper;
        this.resultMapper = resultMapper;
    }

    /**
     * 保存一条翻译结果：主题按「原文 + 源语言 + 目标语言」去重，
     * 同一主题下每个 provider 只保留一条结果，重复提交视为更新。
     */
    @Transactional
    public TopicDetailVO collect(CollectRequest request) {
        String sourceText = request.getSourceText().trim();
        String sourceLang = normalize(request.getSourceLang());
        String targetLang = normalize(request.getTargetLang());
        String provider = normalize(request.getProvider());
        if (provider.isEmpty()) {
            provider = DEFAULT_PROVIDER;
        }
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
        } else {
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
        long pageSize = request.getSize() < 1 ? 20 : Math.min(request.getSize(), 200);
        String keyword = request.getKeyword();

        LambdaQueryWrapper<VocabularyTopic> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(VocabularyTopic::getSourceText, keyword.trim());
        }
        wrapper.orderByDesc(VocabularyTopic::getCreatedAt).orderByDesc(VocabularyTopic::getId);

        Page<VocabularyTopic> pageParam = new Page<>(currentPage, pageSize);
        Page<VocabularyTopic> result = topicMapper.selectPage(pageParam, wrapper);

        List<VocabularyTopic> topics = result.getRecords();
        Map<Long, Long> countMap = countResults(topics);

        List<TopicSummaryVO> records = new ArrayList<>(topics.size());
        for (VocabularyTopic topic : topics) {
            TopicSummaryVO vo = new TopicSummaryVO();
            fillSummary(vo, topic);
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
        fillSummary(vo, topic);

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

    private void fillSummary(TopicSummaryVO vo, VocabularyTopic topic) {
        vo.setId(topic.getId());
        vo.setSourceText(topic.getSourceText());
        vo.setSourceLang(topic.getSourceLang());
        vo.setTargetLang(topic.getTargetLang());
        vo.setCreatedAt(topic.getCreatedAt());
        vo.setUpdatedAt(topic.getUpdatedAt());
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
        return resultMapper.selectOne(new LambdaQueryWrapper<VocabularyResult>()
                .eq(VocabularyResult::getTopicId, topicId)
                .eq(VocabularyResult::getProvider, provider)
                .last("limit 1"));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
