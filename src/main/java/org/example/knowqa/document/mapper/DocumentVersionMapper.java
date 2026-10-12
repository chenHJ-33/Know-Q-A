package org.example.knowqa.document.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.example.knowqa.document.entity.DocumentVersion;

import java.util.List;

@Mapper
public interface DocumentVersionMapper extends BaseMapper<DocumentVersion> {
    @Delete("delete from knowledge_document_version where doc_id=#{docId}")
    int physicalDeleteByDocumentId(Long docId);
    @Delete("<script>DELETE FROM knowledge_document_version WHERE doc_id IN " +
            "<foreach item='docId' collection='docIds' open='(' separator=',' close=')'>#{docId}</foreach></script>")
    void physicalDeleteByDocIds(List<Long> docIds);
}
