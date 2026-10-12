package org.example.knowqa.document.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.example.knowqa.document.entity.Document;

import java.util.List;

@Mapper
public interface DocumentMapper extends BaseMapper<Document> {
    @Delete("delete from knowledge_document where doc_id=#{docId}")
    int physicalDeleteByDocumentId(Long docId);
    @Delete("<script>DELETE FROM knowledge_document WHERE doc_id IN " +
            "<foreach item='docId' collection='docIds' open='(' separator=',' close=')'>#{docId}</foreach></script>")
    int physicalDeleteByDocIds(List<Long> docIds);
}
