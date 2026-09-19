package org.example.knowqa.document.controller;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.example.knowqa.common.DefaultUser;
import org.example.knowqa.document.entity.Document;
import org.example.knowqa.document.entity.DocumentUploadParam;
import org.example.knowqa.document.service.DocumentProcessService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/document")
@Slf4j
public class DocumentController {
    @Resource
    private DocumentProcessService documentProcessService;
    // 上传文档
    @PostMapping("/upload")
    public Document uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam(value = "version", required = false, defaultValue = "1.0.0") String version,
            @RequestParam("description") String description,
            @RequestParam(value = "uploadUser", required = false) String uploadUser
            ) throws IOException {
        return documentProcessService.upload(new DocumentUploadParam(file, title, description, version),
                DefaultUser.userNameOrDefault(uploadUser));
    }
}
