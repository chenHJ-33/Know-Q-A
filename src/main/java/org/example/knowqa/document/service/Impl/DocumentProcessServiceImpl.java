package org.example.knowqa.document.service.Impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.google.common.base.Stopwatch;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.example.knowqa.document.constant.DocumentStatus;
import org.example.knowqa.document.constant.FileType;
import org.example.knowqa.document.constant.SegmentStatus;
import org.example.knowqa.document.entity.*;
import org.example.knowqa.document.event.DocumentChunkedEvent;
import org.example.knowqa.document.mapper.SegmentMapper;
import org.example.knowqa.document.service.*;
import org.example.knowqa.document.util.FileTypeUtil;
import org.example.knowqa.document.util.VersionUtil;
import org.example.knowqa.infra.lock.DistributeLock;
import org.example.knowqa.infra.snowflake.DocumentSplitterFactory;
import org.example.knowqa.infra.snowflake.SnowflakeIdGenerator;
import org.example.knowqa.rag.constant.MetadataKeyConstant;
import org.example.knowqa.rag.sqlitter.ExcelSplitter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StopWatch;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

@Service
@Slf4j
public class DocumentProcessServiceImpl implements DocumentProcessService {
    @Value("${minio.bucketName}")
    private String bucketName;
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
    @Resource
    private ApplicationEventPublisher eventPublisher;

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

    @Override
    @Transactional(rollbackFor = Exception.class)
    @DistributeLock(scene = "document-split",keyExpression = "#document.docId",waitTime = 0)
    public int split(Document document, DocumentSplitParam documentSplitParam) {
        // 查询文档
        Assert.notNull(document,"文档不存在");
        // 从版本表中获取当前版本文件的URL
        DocumentVersion versionRecord = documentVersionService.getById(document.getCurrentVersionId());
        Assert.notNull(versionRecord,"文档版本不存在");
        Assert.notNull(versionRecord.getConvertedDocUrl(),"文档转换为完成");
        if (versionRecord.getStatus() == DocumentStatus.CHUNKED) {
            // 返回已切片的分段数量
            Long chunkedCount=segmentService.count(new QueryWrapper<KnowledgeSegment>()
                    .eq("document_id",document.getDocId())
                    .eq("document_version",document.getCurrentVersionId())
                    .eq("skip_embedding",0));
            return chunkedCount.intValue();
        }
        if (versionRecord.getStatus() != DocumentStatus.CONVERTED) {
            throw new RuntimeException("文档状态不为CONVERTED，无法完成切分");
        }
        // 从minIO下载文件内容
        String convertedDocUrl = versionRecord.getConvertedDocUrl();
        String objectName=extractObjectNameFromUrl(convertedDocUrl);
        Assert.notNull(objectName,"无法解析文档URL");
        List<KnowledgeSegment> knowledgeSegments=new ArrayList<>();
        List<TextSegment> segments;
        try(InputStream inputStream=fileStorageService.downloadFile(objectName)) {
            if (FileType.EXCEL== FileTypeUtil.getFileType(convertedDocUrl)||FileType.CSV==FileTypeUtil.getFileType(convertedDocUrl)){
                ExcelSplitter splitter=new ExcelSplitter(documentSplitParam.getChunkSize(),false);
                segments=splitter.split(inputStream.readAllBytes(),objectName);
            }else {
                DocumentSplitter splitter = DocumentSplitterFactory.getInstance(documentSplitParam);
                dev.langchain4j.data.document.Document doc= dev.langchain4j.data.document.Document.from(new String(inputStream.readAllBytes(), StandardCharsets.UTF_8));
                segments=splitter.split(doc);
            }
        } catch (Exception e) {
            throw new RuntimeException("下载文档失败"+e.getMessage());
        }
        // 转换为KnowledgeSegment并保存
        for (int i=0;i<segments.size();i++) {
            TextSegment segment=segments.get(i);
            Metadata metadata = segment.metadata();
            String chunkId = metadata.getString(MetadataKeyConstant.CHUNK_ID);
            if (chunkId == null || chunkId.isBlank()) {
                chunkId= SnowflakeIdGenerator.getInstance().nextIdStr();
                metadata.put(MetadataKeyConstant.CHUNK_ID,chunkId);
            }
            KnowledgeSegment knowledgeSegment=new KnowledgeSegment();
            knowledgeSegment.setText(segment.text());
            knowledgeSegment.setChunkId(chunkId);
            knowledgeSegment.setMetadata(enrichMetadata(document,versionRecord,metadata));
            knowledgeSegment.setDocumentId(document.getDocId());
            knowledgeSegment.setDocumentVersion(document.getCurrentVersionId());
            knowledgeSegment.setChunkOrder(i);
            // 检查是否需要跳过嵌入
            Integer skipEmbedding = metadata.getInteger(MetadataKeyConstant.SKIP_EMBEDDING);
            if (skipEmbedding != null && skipEmbedding == 1) {
                knowledgeSegment.setSkipEmbedding(1);
                knowledgeSegment.setStatus(SegmentStatus.STORED);
            }else {
                knowledgeSegment.setSkipEmbedding(0);
                knowledgeSegment.setStatus(SegmentStatus.STORED);
            }
            knowledgeSegments.add(knowledgeSegment);
        }
        // 批量保存片段
        Stopwatch stopwatch = Stopwatch.createStarted();
        boolean ok = segmentService.saveBatch(knowledgeSegments);
        Assert.isTrue(ok,"保存知识片段失败");
        log.info("保存知识片段耗时：{}",stopwatch.elapsed().toMillis());
        int segmentCount = knowledgeSegments.size();
        // 更新文档状态为CHUNKED，并保存分段参数
        ok=documentService.advanceDocumentAndVersionStatus(document.getDocId(),document.getCurrentVersionId(),DocumentStatus.CHUNKED);
        Assert.isTrue(ok,"更新文档版本失败");
        // 发送文档已分段事件
        publishChunkEvent(document,segmentCount);
        return segmentCount;
    }

    private void publishChunkEvent(Document document, int segmentCount) {
        log.info("发送文档CHUNKED事件，documentId: {}, segmentCount: {}", document.getDocId(), segmentCount);
        DocumentChunkedEvent event=new DocumentChunkedEvent(this,document.getDocId(),document.getCurrentVersionId(),segmentCount);
        eventPublisher.publishEvent(event);
    }

    private String extractObjectNameFromUrl(String url) {
        if (url==null||url.isEmpty()){
            return null;
        }
        // http://endpoint/bucketName/objectName
        int lastSlashIndex=url.lastIndexOf(bucketName)+bucketName.length();
        if (lastSlashIndex == -1 || lastSlashIndex == url.length() - 1) {
            return null;
        }
        return url.substring(lastSlashIndex+1);
    }

    private static String enrichMetadata(Document document, DocumentVersion versionRecord, Metadata metadata) {
        metadata.put(MetadataKeyConstant.DOC_ID,document.getDocId());
        metadata.put(MetadataKeyConstant.FILE_NAME,document.getDocTitle());
        metadata.put(MetadataKeyConstant.URL,versionRecord.getDocUrl());
        if (document.getCurrentVersionId() != null) {
            metadata.put(MetadataKeyConstant.VERSION,document.getCurrentVersionId());
        }
        return JSON.toJSONString(metadata.toMap());
    }
    @Override
    @DistributeLock(scene = "document-embed",keyExpression = "#documentVersion.versionId",waitTime = 0)
    public boolean embedAndStore(DocumentVersion documentVersion) {
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
