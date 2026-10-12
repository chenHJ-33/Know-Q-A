package org.example.knowqa.document.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.knowqa.document.constant.DocumentStatus;
import org.example.knowqa.document.entity.Document;

import javax.print.Doc;
import java.util.List;

public interface DocumentService extends IService<Document> {
    boolean removeDocumentWithSegments(Long docId);

    boolean advanceDocumentAndVersionStatus(Long docId, Long currentVersionId, DocumentStatus documentStatus);

    void activateVersion(Long versionId);

    void deactivateVersion(Long versionId);

    boolean removeDocumentsWithSegments(List<Long> ids);
}
