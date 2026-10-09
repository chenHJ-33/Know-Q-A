package org.example.knowqa.document.service;

import org.example.knowqa.document.entity.KnowledgeSegment;

import java.util.List;

public interface VectorStoreService {
    void removeByDocId(Long docId);

    List<String> embedAndStore(List<KnowledgeSegment> batch);

    void removeByDocIdAndVersion(Long docId, Long versionId);
}
