package com.zizaizhiwu.vocabulary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zizaizhiwu.vocabulary.dto.CollectionTopicCount;
import com.zizaizhiwu.vocabulary.entity.VocabularyTopicCollection;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface VocabularyTopicCollectionMapper extends BaseMapper<VocabularyTopicCollection> {

    @Select("SELECT collection_id AS collectionId, COUNT(DISTINCT topic_id) AS topicCount " +
            "FROM vocabulary_topic_collection GROUP BY collection_id")
    List<CollectionTopicCount> countTopicsByCollection();

    @Select("SELECT collection_id FROM vocabulary_topic_collection WHERE topic_id = #{topicId}")
    List<Long> selectCollectionIdsByTopicId(@Param("topicId") Long topicId);

    @Select("<script>" +
            "SELECT topic_id, collection_id FROM vocabulary_topic_collection " +
            "WHERE topic_id IN " +
            "<foreach item='id' collection='topicIds' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    List<VocabularyTopicCollection> selectByTopicIds(@Param("topicIds") List<Long> topicIds);

    @Select("SELECT topic_id FROM vocabulary_topic_collection WHERE collection_id = #{collectionId}")
    List<Long> selectTopicIdsByCollectionId(@Param("collectionId") Long collectionId);
}
