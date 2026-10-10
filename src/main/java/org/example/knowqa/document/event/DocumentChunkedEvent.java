package org.example.knowqa.document.event;


import org.springframework.context.ApplicationEvent;

public class DocumentChunkedEvent extends ApplicationEvent {
    private final Long documentId;
    private final Long documentVersionId;
    private final int segmentCount;
    public DocumentChunkedEvent(Object source, Long documentId, Long documentVersionId, int segmentCount) {
        super(source);
        this.documentId = documentId;
        this.documentVersionId = documentVersionId;
        this.segmentCount = segmentCount;
    }
    public Long getDocumentId() {
        return documentId;
    }

    public Long getDocumentVersionId() {
        return documentVersionId;
    }
    public int getSegmentCount() {
        return segmentCount;
    }

    @Override
    public String toString() {
        return "DocumentChunkedEvent{documentId=" + documentId + ", segmentCount=" + segmentCount + '}';
    }
}
