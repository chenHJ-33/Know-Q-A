package org.example.knowqa.document.constant;

import lombok.Getter;

@Getter
public enum FileType {
    PDF("pdf"),
    DOC("doc"),
    TXT("txt"),
    MARKDOWN("markdown"),
    CSV("csv"),
    EXCEL("excel");
    private final String type;
    FileType(String type){
        this.type=type;
    }
}
