package org.example.knowqa.document.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.example.knowqa.document.entity.KnowledgeSegment;

public interface SegmentMapper extends BaseMapper<KnowledgeSegment> {
    @Delete("delete from knowledge_segment where documet_id=#{docId}")
    int physicalDeleteByDocumentId(Long docId);
}
