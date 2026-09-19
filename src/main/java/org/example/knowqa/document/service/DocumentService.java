package org.example.knowqa.document.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.knowqa.document.constant.DocumentStatus;
import org.example.knowqa.document.entity.Document;

import javax.print.Doc;

public interface DocumentService extends IService<Document> {
    boolean removeDocumentWithSegments(Long docId);

    boolean advanceDocumentAndVersionStatus(Long docId, Long currentVersionId, DocumentStatus documentStatus);
}
