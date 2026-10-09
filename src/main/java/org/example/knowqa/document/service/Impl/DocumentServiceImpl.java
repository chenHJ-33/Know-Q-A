package org.example.knowqa.document.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.el.parser.AstSetData;
import org.example.knowqa.document.constant.DocumentStatus;
import org.example.knowqa.document.constant.SegmentStatus;
import org.example.knowqa.document.entity.Document;
import org.example.knowqa.document.entity.DocumentVersion;
import org.example.knowqa.document.entity.KnowledgeSegment;
import org.example.knowqa.document.mapper.DocumentVersionMapper;
import org.example.knowqa.document.mapper.SegmentMapper;
import org.example.knowqa.document.service.*;
import org.example.knowqa.document.mapper.DocumentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.List;

@Service
@Slf4j
public class DocumentServiceImpl extends ServiceImpl<DocumentMapper, Document> implements DocumentService {
    @Resource
    private VectorStoreService vectorStoreService;
    @Resource
    private DocumentVersionService documentVersionService;
    @Resource
    private DocumentVersionMapper documentVersionMapper;
    @Resource
    private SegmentMapper segmentMapper;
    @Resource
    private SegmentService segmentService;
    @Resource
    private DocumentCleanupService documentCleanupService;
    @Autowired
    private DocumentService documentService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeDocumentWithSegments(Long docId) {
        // 删除向量
        deleteVectorsByDocId(docId);
        // 删除分段
        segmentMapper.physicalDeleteByDocumentId(docId);
        // 删除版本记录
        documentVersionMapper.physicalDeleteByDocumentId(docId);
        // 删除文档
        return baseMapper.physicalDeleteByDocumentId(docId)>0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean advanceDocumentAndVersionStatus(Long docId, Long versionId, DocumentStatus targetStatus) {
        Assert.notNull(docId,"文档id不能为空");
        Assert.notNull(versionId,"版本id不能为空");
        Assert.notNull(targetStatus,"目标状态不能为空");
        Document document=this.getById(docId);
        Assert.notNull(document,"文档不存在：docId="+docId);
        DocumentVersion version=documentVersionService.getById(versionId);
        Assert.notNull(version,"版本记录不存在：versionId="+versionId);
        Assert.isTrue(docId.equals(version.getDocId()),"版本不属于该文档");
        boolean updated=false;
        if (shouldAdvanceStatus(document.getStatus(),targetStatus)){
            document.setStatus(targetStatus);
            boolean docResult=this.updateById(document);
            Assert.isTrue(docResult,"文档状态更新失败：docId="+docId);
            updated=true;
            log.info("文档状态以推进，docId={}, status={}, targetStatus={}",
                    docId,document.getStatus(),targetStatus);
        }else {
            log.info("文档状态无需推进，docId={},status={},targetStatus={}",
                    docId,document.getStatus(),targetStatus);
        }
        if (shouldAdvanceStatus(version.getStatus(),targetStatus)){
            version.setStatus(targetStatus);
            boolean versionResult=documentVersionService.updateById(version);
            Assert.isTrue(versionResult,"版本状态更新失败：versionId="+versionId);
            updated=true;
            log.info("版本状态已推进,versionId={},status={}",
                    versionId,targetStatus);
        }else {
            log.info("版本状态无需推进,versionId={},currenStatus={},targetStatus={}",
                    versionId,version.getStatus(),targetStatus);
        }
        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void activateVersion(Long versionId) {
        DocumentVersion version=documentVersionService.getById(versionId);
        Assert.isTrue(DocumentStatus.CHUNKED==version.getStatus(),
                "版本状态不是 CHUNKED，无法执行生效操作，当前状态"+version.getStatus());
        long docId=version.getDocId();
        log.info("开始让版本生效/重新向量化，docId={}, versionId={}",docId,versionId);
        //分页扫描 STORED 且未向量化的分段（skipEmbedding=0）
        LambdaQueryWrapper<KnowledgeSegment> queryWrapper = Wrappers.<KnowledgeSegment>lambdaQuery()
                .eq(KnowledgeSegment::getDocumentId, docId)
                .eq(KnowledgeSegment::getDocumentVersion, versionId)
                .eq(KnowledgeSegment::getStatus, SegmentStatus.STORED)
                .eq(KnowledgeSegment::getSkipEmbedding,0)
                .isNull(KnowledgeSegment::getEmbeddingId);
        Page<KnowledgeSegment> page=segmentService.page(new Page<>(1,100),queryWrapper);
        while (!page.getRecords().isEmpty()) {
            List<KnowledgeSegment> batch = page.getRecords();
            List<String> embeddingIds=vectorStoreService.embedAndStore(batch);
            for (int i = 0; i < batch.size(); i++) {
                KnowledgeSegment seg = batch.get(i);
                seg.setEmbeddingId(embeddingIds.get(i));
                seg.setStatus(SegmentStatus.VECTOR_STORED);
                boolean ok=segmentService.updateById(seg);
                Assert.isTrue(ok,"分段更新失败："+seg.getId());
            }
            page=segmentService.page(new Page<>(page.getCurrent(),100),queryWrapper);
        }
        // 更新版本记录状态
        version.setStatus(DocumentStatus.VECTOR_STORED);
        boolean ok = documentVersionService.updateById(version);
        Assert.isTrue(ok,"版本记录更新失败"+version);
        log.info("版本生效完成，versionId={}",version);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deactivateVersion(Long versionId) {
        DocumentVersion version = documentVersionService.getById(versionId);
        Assert.notNull(version,"版本记录不存在,versionId="+version);
        if (version.getStatus() == DocumentStatus.CHUNKED) {
            return;
        }
        Assert.isTrue(DocumentStatus.VECTOR_STORED == version.getStatus(),
                "版本状态不是 VECTOR_STORED，无法执行失效操作，当前状态: "+version.getStatus());
        Long docId=version.getDocId();
        log.info("开始让版本失效，docId={}，versionId={}", docId, versionId);
        // 根据docId和versionId删除ES中的向量
        documentCleanupService.cleanupOldVersionData(docId,versionId);
        // 将该版本下所有分段状态从 VECTOR_STORED 降为 STORED，并清空 embeddingId
        LambdaUpdateWrapper<KnowledgeSegment> setUpdate=Wrappers.<KnowledgeSegment>lambdaUpdate()
                .set(KnowledgeSegment::getStatus,SegmentStatus.STORED)
                .set(KnowledgeSegment::getEmbeddingId,null)
                .eq(KnowledgeSegment::getDocumentId,docId)
                .eq(KnowledgeSegment::getDocumentVersion,versionId)
                .eq(KnowledgeSegment::getStatus,SegmentStatus.VECTOR_STORED);
        int affected = segmentMapper.update(null, setUpdate);
        log.info("降级分段状态完成，versionId={}，affected={}",versionId,affected);
        // 将版本记录状态从VECTOR_STORED降为 CHUNKED
        version.setStatus(DocumentStatus.CHUNKED);
        boolean ok = documentVersionService.updateById(version);
        Assert.isTrue(ok, "文档版本状态更新失败");
        log.info("版本失效完成，versionId={}",versionId);
    }

    private boolean shouldAdvanceStatus(DocumentStatus status, DocumentStatus targetStatus) {
        if (status==null)return true;
        return status.ordinal()<targetStatus.ordinal();
    }

    private void deleteVectorsByDocId(Long docId) {
        vectorStoreService.removeByDocId(docId);
    }
}
