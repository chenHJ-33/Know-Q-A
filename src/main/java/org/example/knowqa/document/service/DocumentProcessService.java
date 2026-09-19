package org.example.knowqa.document.service;

import org.example.knowqa.document.entity.Document;
import org.example.knowqa.document.entity.DocumentUploadParam;

import java.io.IOException;

public interface DocumentProcessService {
    // 上传文件
    Document upload(DocumentUploadParam documentUploadParam, String s) throws IOException;
}
