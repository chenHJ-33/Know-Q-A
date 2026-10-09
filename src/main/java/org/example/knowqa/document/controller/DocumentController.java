package org.example.knowqa.document.controller;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.example.knowqa.common.DefaultUser;
import org.example.knowqa.document.entity.Document;
import org.example.knowqa.document.entity.DocumentUploadParam;
import org.example.knowqa.document.entity.DocumentVersion;
import org.example.knowqa.document.service.DocumentProcessService;
import org.example.knowqa.document.service.DocumentVersionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/document")
@Slf4j
public class DocumentController {
    @Resource
    private DocumentProcessService documentProcessService;
    @Autowired
    private DocumentVersionService documentVersionService;

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

    // 上传文件新版本
    @PostMapping("/upload-version")
    public Document uploadVersion(
            @RequestParam("file") MultipartFile file,
            @RequestParam("docId") Long docId,
            @RequestParam("version") String version,
            @RequestParam(value = "changelog", required = false) String changelog,
            @RequestParam(value = "uploadUser", required = false) String uploadUser
    ) throws IOException {
        return documentProcessService.uploadNewVersion(docId, version, file, DefaultUser.userNameOrDefault(uploadUser), changelog);
    }

    // 查询文件所有版本
    @GetMapping("/versions/{docId}")
    public List<DocumentVersion> listVersions(@PathVariable Long docId) {
        return documentVersionService.listByDocId(docId);
    }

    // 切换文档到指定版本
    @PostMapping("/switch-version")
    public Document switchVersion(@RequestParam("docId") Long docId,@RequestParam("versionId") Long versionId){
        return documentProcessService.switchVersion(docId,versionId);
    }
}
