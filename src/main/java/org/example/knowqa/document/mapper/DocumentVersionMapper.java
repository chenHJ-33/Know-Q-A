package org.example.knowqa.document.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.example.knowqa.document.entity.DocumentVersion;
@Mapper
public interface DocumentVersionMapper extends BaseMapper<DocumentVersion> {
    @Delete("delete from knowledge_document_version where doc_id=#{docId}")
    int physicalDeleteByDocumentId(Long docId);
}
