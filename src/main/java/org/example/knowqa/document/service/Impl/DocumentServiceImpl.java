package org.example.knowqa.document.service.Impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.example.knowqa.document.constant.DocumentStatus;
import org.example.knowqa.document.entity.Document;
import org.example.knowqa.document.entity.DocumentVersion;
import org.example.knowqa.document.mapper.DocumentVersionMapper;
import org.example.knowqa.document.mapper.SegmentMapper;
import org.example.knowqa.document.service.DocumentService;
import org.example.knowqa.document.service.DocumentVersionService;
import org.example.knowqa.document.service.VectorStoreService;
import org.example.knowqa.document.mapper.DocumentMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

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

    private boolean shouldAdvanceStatus(DocumentStatus status, DocumentStatus targetStatus) {
        if (status==null)return true;
        return status.ordinal()<targetStatus.ordinal();
    }

    private void deleteVectorsByDocId(Long docId) {
        vectorStoreService.removeByDocId(docId);
    }
}
