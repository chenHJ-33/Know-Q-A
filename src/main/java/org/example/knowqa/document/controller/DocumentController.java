package org.example.knowqa.document.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Delete;
import org.example.knowqa.common.DefaultUser;
import org.example.knowqa.document.entity.Document;
import org.example.knowqa.document.entity.DocumentSplitParam;
import org.example.knowqa.document.entity.DocumentUploadParam;
import org.example.knowqa.document.entity.DocumentVersion;
import org.example.knowqa.document.service.DocumentProcessService;
import org.example.knowqa.document.service.DocumentService;
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
    @Autowired
    private DocumentService documentService;

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
    public Document switchVersion(@RequestParam("docId") Long docId, @RequestParam("versionId") Long versionId) {
        return documentProcessService.switchVersion(docId, versionId);
    }

    // 让指定版本生效
    @PostMapping("/activate-version")
    public void activateVersion(@RequestParam("versionId") Long versionId) {
        documentService.activateVersion(versionId);
    }

    // 对文档进行切分
    @PostMapping("/split/{documentId}")
    public Integer splitDocument(
            @PathVariable Long documentId,
            @RequestParam("splitType") String splitType,
            @RequestParam("chunkSize") Integer chunkSize,
            @RequestParam(value = "overlap", required = false) Integer overlap,
            @RequestParam(value = "regex", required = false) String regex,
            @RequestParam(value = "titleLevel", required = false) Integer titleLevel,
            @RequestParam(value = "separator", required = false) String separator

    ) {
        Document document = documentService.getById(documentId);
        return documentProcessService.split(document, new DocumentSplitParam(splitType, chunkSize, overlap, titleLevel, separator, regex));
    }

    // 分页查询
    @GetMapping("/page")
    public Page<Document> page(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(value = "docTitle", required = false) String docTitle,
            @RequestParam(value = "status", required = false) String status
    ) {
        Page<Document> page = new Page<>(current, size);
        QueryWrapper<Document> queryWrapper = new QueryWrapper<>();
        if (docTitle != null) {
            queryWrapper.like("doc_title", docTitle);
        }
        if (status != null) {
            queryWrapper.eq("status", status);
        }
        queryWrapper.orderByDesc("created_at");
        return documentService.page(page, queryWrapper);
    }

    // 根据ID查询文档详情
    @GetMapping("/{id:\\d+}")
    public Document getById(@PathVariable Long id) {
        return documentService.getById(id);
    }

    // 新增文档记录
    @PostMapping
    public boolean save(@RequestBody Document document) {
        return documentService.save(document);
    }

    // 根据ID更新文档
    @PutMapping
    public boolean updateById(@RequestBody Document document) {
        return documentService.updateById(document);
    }

    // 根据ID删除文档（逻辑删除，并级联删除该文档下的所有分段及向量）
    @DeleteMapping("/{id:\\d+}")
    public boolean removeById(@PathVariable Long id) {
        return documentService.removeDocumentWithSegments(id);
    }

    // 批量删除文档
    @DeleteMapping("/batch")
    public boolean removeByIds(@RequestParam List<Long> ids) {
        return documentService.removeDocumentsWithSegments(ids);
    }
}
