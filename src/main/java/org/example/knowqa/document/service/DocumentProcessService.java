package org.example.knowqa.document.service;

import org.example.knowqa.document.entity.Document;
import org.example.knowqa.document.entity.DocumentSplitParam;
import org.example.knowqa.document.entity.DocumentUploadParam;
import org.example.knowqa.document.entity.DocumentVersion;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface DocumentProcessService {
    // 上传文件
    Document upload(DocumentUploadParam documentUploadParam, String s) throws IOException;
    // 上传文件新版本
    Document uploadNewVersion(Long docId, String version, MultipartFile file, String s, String changelog) throws IOException;

    Document switchVersion(Long docId, Long versionId);

    int split(Document document, DocumentSplitParam documentSplitParam);
}
