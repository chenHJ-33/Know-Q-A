package org.example.knowqa.document.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DocumentSplitParam {
    private String splitType;
    private Integer chunkSize;
    private Integer overlap;
    private Integer titleLevel;
    private String separator;
    private String regex;
}
