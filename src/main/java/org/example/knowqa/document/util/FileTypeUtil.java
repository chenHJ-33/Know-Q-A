package org.example.knowqa.document.util;

import org.example.knowqa.document.constant.FileType;

public class FileTypeUtil {

    public static FileType getFileType(String fileName) {
        String extension=extractExtension(fileName);
        return switch (extension) {
            case "pdf"->FileType.PDF;
            case "csv"->FileType.CSV;
            case "xlsx","xls"->FileType.EXCEL;
            case "docx","doc"->FileType.DOC;
            case "md"->FileType.MARKDOWN;
            case "txt"->FileType.TXT;
            default ->throw new IllegalArgumentException("不支持的文件类型： ."+extension);
        };
    }

    private static String extractExtension(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("文件名不能为空");
        }
        String name=fileName;
        int queryIndex=name.indexOf('?');
        if (queryIndex >= 0) {
            name=name.substring(0,queryIndex);
        }
        int lastDot=name.lastIndexOf('.');
        if (lastDot < 0 || lastDot == name.length() - 1) {
            throw new IllegalArgumentException("文件名缺少后缀： "+fileName);
        }
        return name.substring(lastDot+1).toLowerCase();
    }

}
