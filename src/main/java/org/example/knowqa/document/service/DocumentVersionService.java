package org.example.knowqa.document.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.apache.ibatis.annotations.Delete;
import org.example.knowqa.document.entity.DocumentVersion;

public interface DocumentVersionService extends IService<DocumentVersion> {
    boolean existsByContentHash(String contentHash);


}
