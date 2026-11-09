package com.zizaizhiwu.vocabulary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zizaizhiwu.vocabulary.dto.TopicResultCount;
import com.zizaizhiwu.vocabulary.entity.VocabularyResult;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface VocabularyResultMapper extends BaseMapper<VocabularyResult> {

    @Select("<script>" +
            "SELECT topic_id AS topicId, COUNT(*) AS resultCount FROM vocabulary_result " +
            "WHERE topic_id IN " +
            "<foreach item='id' collection='ids' open='(' separator=',' close=')'>#{id}</foreach> " +
            "GROUP BY topic_id" +
            "</script>")
    List<TopicResultCount> countByTopicIds(@Param("ids") List<Long> ids);
}
