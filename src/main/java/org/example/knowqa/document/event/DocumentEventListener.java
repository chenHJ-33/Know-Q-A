package org.example.knowqa.document.event;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.example.knowqa.document.constant.DocumentStatus;
import org.example.knowqa.document.entity.Document;
import org.example.knowqa.document.entity.DocumentVersion;
import org.example.knowqa.document.service.DocumentProcessService;
import org.example.knowqa.document.service.DocumentService;
import org.example.knowqa.document.service.DocumentVersionService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.util.Assert;

@Slf4j
@Component
public class DocumentEventListener {
    @Resource
    private DocumentProcessService documentProcessService;
    @Resource
    private DocumentService documentService;
    @Resource
    private DocumentVersionService documentVersionService;

    @Async("eventListenerExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDocumentChunked(DocumentChunkedEvent event) {
        Long documentId = event.getDocumentId();
        log.info("收到文档CHUNKED事件（事务已提交），开始执行向量嵌入，documentId: {}, segmentCount: {}",
                documentId, event.getSegmentCount());
        try {
            DocumentVersion documentVersion = documentVersionService.getById(event.getDocumentVersionId());
            boolean ok = documentProcessService.embedAndStore(documentVersion);
            Assert.isTrue(ok, "向量嵌入失败：documentId=" + documentId);
            log.info("向量嵌入完成，documentId：{}，ok：{}", documentId, ok);
            Document document = documentService.getById(documentId);
            if (document != null && event.getDocumentVersionId().equals(document.getCurrentVersionId())) {
                document.setStatus(DocumentStatus.VECTOR_STORED);
                ok = documentService.updateById(document);
                Assert.isTrue(ok, "文档更新状态失败：docId=" + documentId);
                log.info("文档状态已同步为VECTOR_STORED,docId={}", documentId);
            }
        } catch (Exception e) {
            log.error("向量嵌入失败，documentId：{}",documentId,e);
        }
    }
}
