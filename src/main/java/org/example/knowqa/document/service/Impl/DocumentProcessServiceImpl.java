package org.example.knowqa.document.service.Impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.example.knowqa.document.constant.DocumentStatus;
import org.example.knowqa.document.constant.SegmentStatus;
import org.example.knowqa.document.entity.Document;
import org.example.knowqa.document.entity.DocumentUploadParam;
import org.example.knowqa.document.entity.DocumentVersion;
import org.example.knowqa.document.entity.KnowledgeSegment;
import org.example.knowqa.document.mapper.SegmentMapper;
import org.example.knowqa.document.service.*;
import org.example.knowqa.document.util.VersionUtil;
import org.example.knowqa.infra.lock.DistributeLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

@Service
@Slf4j
public class DocumentProcessServiceImpl implements DocumentProcessService {
    @Resource
    private DocumentVersionService documentVersionService;
    @Resource
    private DocumentService documentService;
    @Resource
    private FileStorageService fileStorageService;
    @Resource
    private SegmentMapper segmentMapper;
    @Resource
    private SegmentService segmentService;

    @Override
    @DistributeLock(scene = "document-upload",keyExpression = "#uploadUser",waitTime = 0)
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

    @Override
    @DistributeLock(scene = "document-upload",keyExpression = "#uploadUser",waitTime = 0)
    public Document uploadNewVersion(Long docId, String version, MultipartFile file, String uploadUser, String changelog) throws IOException {
        // 查询文档
        Document document=documentService.getById(docId);
        Assert.notNull(document,"文档不存在");
        // 校验版本号》最大版本号
        String latestVersion=documentVersionService.getLatestVersion(docId);
        if (latestVersion!=null&& VersionUtil.compareVersion(version,latestVersion)<=0){
            throw  new IllegalArgumentException("版本号"+version+" 不大于现有最大版本号 "+latestVersion);
        }
        // 计算hash
        String contentHash = calculateContentHash(file);
        // 检查是否存在相同
        if (documentVersionService.existsByContentHash(contentHash)){
            throw new IllegalArgumentException("文档已存在，请勿重复上传");
        }
        DocumentVersion versionRecord=null;
        log.info("start to upload version {} for doc {} ...",version,docId);
        // 上传minIO
        String fileName = file.getName();
        String fileURL=null;
        try {
            fileURL = fileStorageService.uploadFile(file, fileName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        // 创建文档记录
        versionRecord = createVersionRecord(document.getDocId(), version, fileURL, null, uploadUser, contentHash, DocumentStatus.UPLOADED, changelog);
        document.setCurrentVersionId(versionRecord.getVersionId());
        // 处理文档获取URL
        String convertedDocUrl = processFile(fileName, file, document, fileURL);
        // 更新URL
        versionRecord=documentVersionService.getById(versionRecord.getVersionId());
        versionRecord.setConvertedDocUrl(convertedDocUrl);
        boolean ok=documentVersionService.updateById(versionRecord);
        Assert.isTrue(ok,"版本记录更新失败");
        ok=documentService.updateById(document);
        Assert.isTrue(ok,"文档当前版本更新失败");
        log.info("文档{}新版本{}上传完成，旧版本数据保留中，待新版本向量化后清理",docId,version);
        return document;
    }

    @Override
    @DistributeLock(scene = "document-upload",keyExpression = "#docId",waitTime = 0)
    @Transactional(rollbackFor = Exception.class)
    public Document switchVersion(Long docId, Long versionId) {
        // 查询文档
        Document document = documentService.getById(docId);
        Assert.notNull(document,"文档不存在");
        // 查询目标版本
        DocumentVersion versionRecord = documentVersionService.getById(versionId);
        Assert.notNull(versionRecord,"版本不存在");
        Assert.isTrue(versionRecord.getDocId().equals(docId),"版本不属于该文档");
        // 对比
        // 当前版本等于目标版本
        if (versionId.equals(document.getCurrentVersionId())) {
            return document;
        }
        // 切换
        log.info("切换文档 {} 的版本：从 versionId={} 切换到 versionId={}", docId, document.getCurrentVersionId(), versionId);
        LambdaUpdateWrapper<KnowledgeSegment> updateWrapper = Wrappers.<KnowledgeSegment>lambdaUpdate()
                .set(KnowledgeSegment::getStatus, SegmentStatus.STORED)
                .eq(KnowledgeSegment::getDocumentId, document.getDocId())
                .eq(KnowledgeSegment::getDocumentVersion, document.getCurrentVersionId());
        int segAffected=segmentMapper.update(null,updateWrapper);
        log.info("切换版本：旧版本分段状态降级完成, affected={}", segAffected);

        boolean ok=embedAndStore(versionRecord);
        Assert.isTrue(ok,"更新文档片段状态失败");

        // 更新文档
        document.setCurrentVersionId(versionId);
        ok = documentService.updateById(document);
        Assert.isTrue(ok,"更新文档版本失败");
        return document;
    }
    @DistributeLock(scene = "document-embed",keyExpression = "#documentVersion.versionId",waitTime = 0)
    private boolean embedAndStore(DocumentVersion documentVersion) {
        if (documentVersion==null)return false;
        if (documentVersion.getStatus() == DocumentStatus.VECTOR_STORED) {
            log.info("文档版本状态已为VECTOR_STORED，无需重复向量化: {}", documentVersion.getVersionId());
            return true;
        }
        if (documentVersion.getStatus() != DocumentStatus.CHUNKED) {
            log.warn("文档版本状态不是CHUNKED，无法完成向量化: {}", documentVersion.getStatus());
            return false;
        }
        documentService.activateVersion(documentVersion.getVersionId());
        // 检查
        long segmentCount=segmentService.count(new QueryWrapper<KnowledgeSegment>()
                .eq("document_id",documentVersion.getDocId())
                .eq("document_version",documentVersion.getVersionId())
                .eq("status",SegmentStatus.STORED)
                .eq("skip_embedding",0)
        );
        if (segmentCount == 0) {
            // 针对非当前版本的文档，取消激活
            List<DocumentVersion> documentVersions = documentVersionService.list(new QueryWrapper<DocumentVersion>()
                    .eq("doc_id", documentVersion.getDocId())
                    .eq("status", DocumentStatus.VECTOR_STORED)
                    .ne("version_id", documentVersion.getVersionId()));

            documentVersions.forEach(version -> documentService.deactivateVersion(version.getVersionId()));
            return true;
        }
        log.warn("向量存储失败，存在部分分段没有存储成功，未成功的数量： " + segmentCount);
        return false;
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
