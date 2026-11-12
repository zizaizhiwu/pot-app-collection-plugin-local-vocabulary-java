package com.zizaizhiwu.vocabulary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zizaizhiwu.vocabulary.dto.CollectionTopicCount;
import com.zizaizhiwu.vocabulary.entity.VocabularyCollection;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface VocabularyCollectionMapper extends BaseMapper<VocabularyCollection> {

    @Select("SELECT collection_id AS collectionId, COUNT(*) AS topicCount " +
            "FROM vocabulary_topic WHERE collection_id IS NOT NULL GROUP BY collection_id")
    List<CollectionTopicCount> countTopicsByCollection();
}
