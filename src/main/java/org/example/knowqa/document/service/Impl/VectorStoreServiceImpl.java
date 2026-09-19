package org.example.knowqa.document.service.Impl;

import com.baomidou.mybatisplus.extension.service.IService;
import dev.langchain4j.store.embedding.elasticsearch.ElasticsearchEmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.util.validation.metadata.MetadataContext;
import org.example.knowqa.document.service.VectorStoreService;
import org.example.knowqa.rag.constant.MetadataKeyConstant;
import org.springframework.stereotype.Service;

import static dev.langchain4j.store.embedding.filter.MetadataFilterBuilder.metadataKey;
@Service
@Slf4j
public class VectorStoreServiceImpl implements VectorStoreService {
    @Resource
    private ElasticsearchEmbeddingStore embeddingStore;

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
}
