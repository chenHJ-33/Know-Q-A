package org.example.knowqa.document.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.example.knowqa.document.entity.KnowledgeSegment;

import java.util.List;

@Mapper
public interface SegmentMapper extends BaseMapper<KnowledgeSegment> {
    @Delete("delete from knowledge_segment where documet_id=#{docId}")
    int physicalDeleteByDocumentId(Long docId);
    @Delete("<script>DELETE FROM knowledge_segment WHERE document_id IN " +
            "<foreach item='docId' collection='docIds' open='(' separator=',' close=')'>#{docId}</foreach></script>")
    void physicalDeleteByDocumentIds(List<Long> docIds);
}
