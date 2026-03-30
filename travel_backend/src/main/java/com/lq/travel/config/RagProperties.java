package com.lq.travel.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * RAG 配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai.rag")
public class RagProperties {

    /**
     * 是否启用 RAG
     */
    private boolean enabled = true;

    /**
     * 检索返回的候选条数
     */
    private int topK = 6;

    /**
     * 单条知识片段的最大长度
     */
    private int snippetMaxLength = 280;

    /**
     * Milvus 配置
     */
    private MilvusProperties milvus = new MilvusProperties();

    @Data
    public static class MilvusProperties {

        /**
         * 是否启用 Milvus 检索
         */
        private boolean enabled = false;

        /**
         * Milvus REST Endpoint，例如 http://127.0.0.1:19530
         */
        private String endpoint = "http://127.0.0.1:19530";

        /**
         * 鉴权 token，例如 root:Milvus
         */
        private String token;

        /**
         * 逻辑数据库名称
         */
        private String database = "default";

        /**
         * 向量集合名称
         */
        private String collection = "travel_knowledge";

        /**
         * 向量字段名
         */
        private String vectorField = "embedding";

        /**
         * 检索后返回的字段列表
         */
        private List<String> outputFields = new ArrayList<>(List.of(
                "content",
                "title",
                "name",
                "source",
                "city",
                "category"
        ));

        /**
         * Milvus 检索超时时间（毫秒）
         */
        private int searchTimeoutMs = 8000;

        /**
         * HNSW/IVF 类索引的 nprobe 参数
         */
        private int nprobe = 16;

        /**
         * 启动时是否自动初始化集合
         */
        private boolean autoInitCollection = true;

        /**
         * 启动时是否自动触发全量知识库同步
         */
        private boolean autoSyncOnStartup = false;

        /**
         * 全量同步每批写入大小
         */
        private int syncBatchSize = 20;

        /**
         * 单张知识表最多同步条数
         */
        private int syncLimitPerTable = 300;

        /**
         * 向量集合主键字段
         */
        private String primaryField = "id";

        /**
         * 启动初始化最大重试次数
         */
        private int startupInitRetryTimes = 6;

        /**
         * 启动初始化重试间隔（毫秒）
         */
        private long startupInitRetryIntervalMs = 3000L;
    }
}
