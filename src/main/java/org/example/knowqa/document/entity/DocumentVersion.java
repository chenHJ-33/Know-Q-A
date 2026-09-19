package org.example.knowqa.document.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.example.knowqa.document.constant.DocumentStatus;

@Data
@TableName("knowledge_document_version")
public class DocumentVersion {
    private Long versionId;
    private Long docId;
    private String version;
    private String convertedDocUrl;
    private String docUrl;
    private String contentHash;
    private DocumentStatus status;
    private String uploadUser;
    private String changelog;
}
