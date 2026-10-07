package org.example.knowqa.document.service.Impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.knowqa.document.entity.DocumentVersion;
import org.example.knowqa.document.service.DocumentVersionService;
import org.example.knowqa.document.mapper.DocumentVersionMapper;
import org.example.knowqa.document.util.VersionUtil;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class DocumentVersionServiceImpl extends ServiceImpl<DocumentVersionMapper, DocumentVersion> implements DocumentVersionService {
    @Override
    public boolean existsByContentHash(String contentHash) {
        return lambdaQuery().eq(DocumentVersion::getContentHash,contentHash).count()>0;
    }

    @Override
    public String getLatestVersion(Long docId) {
        List<DocumentVersion> versions=listByDocId(docId);
        if (versions.isEmpty()){
            return null;
        }
        return versions.get(0).getVersion();
    }

    @Override
    public List<DocumentVersion> listByDocId(Long docId) {
        List<DocumentVersion> versions = lambdaQuery().eq(DocumentVersion::getDocId, docId).list();
        versions.sort(VERSION_COMPARATOR.reversed());
        return versions;
    }


    private static final Comparator<DocumentVersion> VERSION_COMPARATOR =
            Comparator.comparing(DocumentVersion::getVersion, VersionUtil::compareVersion);
}
