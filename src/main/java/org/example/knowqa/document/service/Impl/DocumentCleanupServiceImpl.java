package org.example.knowqa.document.service.Impl;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.example.knowqa.document.service.DocumentCleanupService;
import org.example.knowqa.document.service.VectorStoreService;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class DocumentCleanupServiceImpl implements DocumentCleanupService {
    @Resource
    private VectorStoreService vectorStoreService;
    @Override
    public boolean cleanupOldVersionData(Long docId, Long versionId) {
        log.info("开始清理文档{}的旧版本数据（保留versionId={}）",docId,versionId);
        vectorStoreService.removeByDocIdAndVersion(docId,versionId);
        log.info("清理文档{}旧版本向量完成",docId);
        return true;
    }
}
