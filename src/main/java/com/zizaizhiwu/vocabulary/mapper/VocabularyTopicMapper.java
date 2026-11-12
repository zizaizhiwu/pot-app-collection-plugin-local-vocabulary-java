package com.zizaizhiwu.vocabulary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zizaizhiwu.vocabulary.dto.LanguageCountVO;
import com.zizaizhiwu.vocabulary.entity.VocabularyTopic;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface VocabularyTopicMapper extends BaseMapper<VocabularyTopic> {

    @Select("SELECT source_lang AS code, COUNT(*) AS `count` " +
            "FROM vocabulary_topic GROUP BY source_lang ORDER BY `count` DESC")
    List<LanguageCountVO> countBySourceLang();
}
