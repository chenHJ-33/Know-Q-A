package org.example.knowqa.document.service.Impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.example.knowqa.document.entity.KnowledgeSegment;
import org.example.knowqa.document.mapper.SegmentMapper;
import org.example.knowqa.document.service.SegmentService;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SegmentServiceImpl extends ServiceImpl<SegmentMapper, KnowledgeSegment> implements SegmentService {
}
