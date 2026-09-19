package org.example.knowqa.document.service.Impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.knowqa.document.entity.DocumentVersion;
import org.example.knowqa.document.service.DocumentVersionService;
import org.example.knowqa.document.mapper.DocumentVersionMapper;
import org.springframework.stereotype.Service;

@Service
public class DocumentVersionServiceImpl extends ServiceImpl<DocumentVersionMapper, DocumentVersion> implements DocumentVersionService {
    @Override
    public boolean existsByContentHash(String contentHash) {
        return lambdaQuery().eq(DocumentVersion::getContentHash,contentHash).count()>0;
    }
}
