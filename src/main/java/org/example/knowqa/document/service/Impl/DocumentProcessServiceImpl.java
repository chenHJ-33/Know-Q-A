package org.example.knowqa.document.service.Impl;

import jakarta.annotation.Resource;
import kotlin.io.path.IllegalFileNameException;
import lombok.extern.slf4j.Slf4j;
import org.example.knowqa.document.constant.DocumentStatus;
import org.example.knowqa.document.entity.Document;
import org.example.knowqa.document.entity.DocumentUploadParam;
import org.example.knowqa.document.entity.DocumentVersion;
import org.example.knowqa.document.service.DocumentProcessService;
import org.example.knowqa.document.service.DocumentService;
import org.example.knowqa.document.service.DocumentVersionService;
import org.example.knowqa.document.service.FileStorageService;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Service
@Slf4j
public class DocumentProcessServiceImpl implements DocumentProcessService {
    @Resource
    private DocumentVersionService documentVersionService;
    @Resource
    private DocumentService documentService;
    @Resource
    private FileStorageService fileStorageService;
    @Override
    public Document upload(DocumentUploadParam documentUploadParam, String uploadUser) throws IOException {
        // 计算文件hash
        String contentHash=calculateContentHash(documentUploadParam.getFile());
        // 检查是否存在相同内容的版本
        if (documentVersionService.existsByContentHash(contentHash)){
            throw new IllegalArgumentException("文档已存在请勿重复上传");
        }
        // 创建文档记录
        Document document=new Document().create(documentUploadParam);
        boolean ok = documentService.save(document);
        Assert.isTrue(ok,"文件上传失败");

        log.info("start to upload ....");
        String fileName = documentUploadParam.getFile().getOriginalFilename();
        String fileUrl;
        try {
            fileUrl=fileStorageService.uploadFile(documentUploadParam.getFile(),fileName);
        }catch (Exception e){
            documentService.removeDocumentWithSegments(document.getDocId());
            log.info("文件上传失败，文档已删除");
            return null;
        }
        // 创建初始版本记录
        DocumentVersion versionRecord=createVersionRecord(
                document.getDocId(), documentUploadParam.getVersion(), fileUrl, null,
                uploadUser, contentHash, DocumentStatus.UPLOADED, null
        );
        document.setCurrentVersionId(versionRecord.getVersionId());
        // 处理文档，获取转换后的url
        String convertedDocUrl=processFile(fileName, documentUploadParam.getFile(), document, fileUrl);
        // 更新版本记录的转换后的url
        versionRecord=documentVersionService.getById(versionRecord.getVersionId());
        versionRecord.setConvertedDocUrl(convertedDocUrl);
        ok=documentVersionService.updateById(versionRecord);
        Assert.isTrue(ok,"版本记录更新失败");
        Document documentInDb=documentService.getById(document.getDocId());
        documentInDb.setCurrentVersionId(versionRecord.getVersionId());
        ok=documentService.updateById(documentInDb);
        Assert.isTrue(ok,"文档当前版本更新失败");
        return document;
    }

    private String processFile(String fileName, MultipartFile documentUploadParam, Document document, String fileUrl)throws IOException {
        String convertedDocUrl;
//        FileProcessService fileProcessService=fileProcessServiceFactory.get(FileTypeUtil.getFileType(fileName));
        if (false){// fileProcessService!=null
            // todo PDF/Word/MD
        }else {
            documentService.advanceDocumentAndVersionStatus(document.getDocId(),document.getCurrentVersionId(),DocumentStatus.CONVERTED);
            document.setStatus(DocumentStatus.CONVERTED);
            convertedDocUrl=fileUrl;
        }
        return convertedDocUrl;
    }

    private DocumentVersion createVersionRecord(Long docId, String version, String docUrl,
                                                String convertedDocUrl, String uploadUser, String contentHash,
                                                DocumentStatus status, String  changelog) {
        DocumentVersion versionRecord=new DocumentVersion();
        versionRecord.setDocId(docId);
        versionRecord.setVersion(version);
        versionRecord.setDocUrl(docUrl);
        versionRecord.setConvertedDocUrl(convertedDocUrl);
        versionRecord.setContentHash(contentHash);
        versionRecord.setStatus(status);
        versionRecord.setUploadUser(uploadUser);
        versionRecord.setChangelog(changelog);
        documentVersionService.save(versionRecord);
        log.info("创建版本记录成功, docId: {}, version: {}, versionId: {}",
                docId, version, versionRecord.getVersionId());
        return versionRecord;

    }

    private String calculateContentHash(MultipartFile file) throws IOException {
        try(InputStream is=file.getInputStream()) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer=new byte[8192];
            int bytesRead;
            while ((bytesRead=is.read(buffer))!=-1){
                digest.update(buffer,0,bytesRead);
            }
            return HexFormat.of().formatHex(digest.digest());
        }catch (NoSuchAlgorithmException e){
            throw new IOException("SHA-256算法不可用",e);
        }
    }
}
