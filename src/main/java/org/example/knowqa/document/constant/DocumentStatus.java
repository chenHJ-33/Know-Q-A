package org.example.knowqa.document.constant;

public enum DocumentStatus {
    /**
     * 上传完成
     */
    UPLOADED,
    /**
     * 转换中
     */
    CONVERTING,
    /**
     * 转换完成
     */
    CONVERTED,
    /**
     * 分块完成
     */
    CHUNKED,
    /**
     * 向量存储完成
     */
    VECTOR_STORED;
}
