package org.example.knowqa.document.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.example.knowqa.document.entity.KnowledgeSegment;
import org.example.knowqa.document.service.SegmentService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/segment")
public class SegmentController {
    @Resource
    private SegmentService segmentService;

    // 根据Id查询
    @GetMapping("/{id}")
    public KnowledgeSegment getById(@PathVariable Long id) {
        return segmentService.getById(id);
    }

    // 根据文档id分页查询片段
    @GetMapping("/page-by-document")
    public Page<KnowledgeSegment> pageByDocument(
            @RequestParam Long documentId,
            @RequestParam(required = false) Long documentVersion,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size
    ) {
        Page<KnowledgeSegment> page = new Page<>();
        QueryWrapper<KnowledgeSegment> wrapper = new QueryWrapper<>();
        wrapper.eq("document_id", documentId);
        if (documentVersion != null) {
            wrapper.eq("document_version", documentVersion);
        }
        wrapper.orderByAsc("chunk_order");
        return segmentService.page(page, wrapper);
    }

    // 根据文档id统计片段数量
    @GetMapping("/count-by-document")
    public long countByDocumentId(@RequestParam Long documentId, @RequestParam(required = false) Long documentVersion) {
        QueryWrapper<KnowledgeSegment> wrapper = new QueryWrapper<>();
        wrapper.eq("document_id", documentId);
        if (documentVersion != null) {
            wrapper.eq("document_version", documentVersion);
        }
        return segmentService.count(wrapper);
    }

    // 根据id更新
    @PutMapping
    public boolean updateById(@RequestBody KnowledgeSegment segment) {
        KnowledgeSegment existing = segmentService.getById(segment.getId());
        if (existing != null && existing.getSkipEmbedding() != null && existing.getSkipEmbedding() == 1) {
            throw new IllegalArgumentException("父分段不支持直接修改，请修改对应的子分段");
        }
        return segmentService.updateById(segment);
    }

    // 根据id删除
    @DeleteMapping("/{id}")
    public boolean deleteById(@PathVariable Long id){
        return segmentService.removeById(id);
    }
}
