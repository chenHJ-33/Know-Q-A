package org.example.knowqa.document.service.Impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.example.knowqa.document.constant.DocumentStatus;
import org.example.knowqa.document.entity.Document;
import org.example.knowqa.document.service.DocumentService;
import org.example.knowqa.document.service.VectorStoreService;
import org.example.knowqa.document.mapper.DocumentMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DocumentServiceImpl extends ServiceImpl<DocumentMapper, Document> implements DocumentService {
    @Resource
    private VectorStoreService vectorStoreService;
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeDocumentWithSegments(Long docId) {
        // todo removeDocumentWithSegments
        // 删除向量
        deleteVectorsByDocId(docId);
        // 删除分段

        return false;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean advanceDocumentAndVersionStatus(Long docId, Long currentVersionId, DocumentStatus documentStatus) {
        // todo advanceDocumentAndVersionStatus
        return false;
    }

    private void deleteVectorsByDocId(Long docId) {
        vectorStoreService.removeByDocId(docId);
    }
}
