package org.example.knowqa.document.entity;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.example.knowqa.document.constant.SegmentStatus;

import java.util.Map;

@Data
@TableName("knowledge_segment")
public class KnowledgeSegment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String text;
    private String chunkId;
    private String metadata;
    private Long documentId;
    private Long documentVersion;
    private Integer chunkOrder;
    private String embeddingId;
    private SegmentStatus status;
    private Integer skipEmbedding;
    @JsonIgnore
    public Map<String,String> getMetadataMap(){
        return metadata==null?null: JSON.parseObject(metadata,Map.class);
    }
}
