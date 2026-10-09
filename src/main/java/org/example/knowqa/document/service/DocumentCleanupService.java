package org.example.knowqa.document.service;

public interface DocumentCleanupService {
    boolean cleanupOldVersionData(Long docId, Long versionId);
}
