package org.example.knowqa.document.service.Impl;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.elasticsearch.ElasticsearchEmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.example.knowqa.document.entity.KnowledgeSegment;
import org.example.knowqa.document.service.VectorStoreService;
import org.example.knowqa.rag.constant.MetadataKeyConstant;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static dev.langchain4j.store.embedding.filter.MetadataFilterBuilder.metadataKey;
@Service
@Slf4j
public class VectorStoreServiceImpl implements VectorStoreService {
    @Resource
    private ElasticsearchEmbeddingStore embeddingStore;
    @Resource
    private OpenAiEmbeddingModel openAiEmbeddingModel;

    @Override
    public void removeByDocId(Long docId) {
        try {
            Filter filter=metadataKey(MetadataKeyConstant.DOC_ID).isEqualTo(docId);
            embeddingStore.removeAll(filter);
            log.info("按docId删除向量成功, docId: {}", docId);
        }catch (Exception e){
            log.warn("按docId删除向量失败, docId: {}, error: {}", docId, e.getMessage());
        }
    }

    @Override
    public List<String> embedAndStore(List<KnowledgeSegment> segments) {
        if (CollectionUtils.isEmpty(segments)) {
            return Collections.emptyList();
        }
        List<TextSegment> textSegment = segments.stream().map(this::toTextSegment).toList();
        Response<List<Embedding>> embeddingResponse = openAiEmbeddingModel.embedAll(textSegment);
        List<String> embeddingIds = embeddingStore.addAll(embeddingResponse.content(), textSegment);
        Assert.isTrue(embeddingIds.size()==segments.size(),
                "向量存储失败，向量数量与分段数量不一致");
        log.info("批量向量化完成，count：{}",segments.size());
        return embeddingIds;
    }

    @Override
    public void removeByDocIdAndVersion(Long docId, Long versionId) {
        try {
            Filter filter = metadataKey(MetadataKeyConstant.DOC_ID).isEqualTo(docId).and(metadataKey(MetadataKeyConstant.VERSION).isEqualTo(versionId));
            embeddingStore.removeAll(filter);
            log.info("按docId+versionId删除向量成功，docId：{}，versionId：{}", docId, versionId);
        } catch (Exception e) {
            log.warn("按docId+versionId删除向量失败，docId：{}，versionId：{}，err：{}",
                    docId, versionId,e.getMessage());
        }
    }

    private TextSegment toTextSegment(KnowledgeSegment segment) {
        Map<String, String> metadataMap = segment.getMetadataMap();
        Metadata metadata=metadataMap!=null?Metadata.from(metadataMap):new Metadata();
        return TextSegment.from(segment.getText(),metadata);
    }
}
