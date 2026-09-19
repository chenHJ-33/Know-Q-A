package org.example.knowqa.rag.constant;

public class MetadataKeyConstant {
    /**
     * 文件名称
     */
    public static final String FILE_NAME = "fileName";


    public static final String DOC_ID = "docId";

    public static final String CHUNK_ID = "chunkId";

    public static final String EMBEDDING_ID = "EMBEDDING_ID";

    /**
     * 父块ID
     */
    public static final String PARENT_CHUNK_ID = "parentChunkId";

    /**
     * 头级别
     */
    public static final String HEADER_LEVEL = "headerLevel";

    /**
     * 文件地址
     */
    public static final String URL = "url";

    /**
     * 文件版本
     */
    public static final String VERSION = "version";

    /**
     * 分类
     */
    public static final String CATEGORY = "category";

    /**
     * 摘要
     */
    public static final String SUMMARY = "summary";

    /**
     * 关键字
     */
    public static final String KEYWORDS = "keywords";

    /**
     * 跳过embedding标记，true表示不需要做embedding
     */
    public static final String SKIP_EMBEDDING = "skipEmbedding";

    /**
     * RAG 检索来源，取值见
     */
    public static final String RETRIEVAL_SOURCE = "retrievalSource";
}
