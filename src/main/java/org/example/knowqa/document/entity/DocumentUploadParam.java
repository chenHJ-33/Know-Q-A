package org.example.knowqa.document.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
@AllArgsConstructor
public class DocumentUploadParam {
    private MultipartFile file;
    private String title;
    private String description;
    private String version;
}
