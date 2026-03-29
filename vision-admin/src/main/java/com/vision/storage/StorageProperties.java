package com.vision.storage;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 存储配置属性
 * 绑定 application.yml 中 vision.storage 前缀的配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "vision.storage")
public class StorageProperties {

    /**
     * 存储类型：local 或 minio
     */
    private String type = "local";

    /**
     * 本地存储配置
     */
    private Local local = new Local();

    /**
     * MinIO 存储配置
     */
    private Minio minio = new Minio();

    @Data
    public static class Local {
        /**
         * 本地存储基础路径
         */
        private String basePath = "/data/vision/files";

        /**
         * 访问基础 URL
         */
        private String baseUrl = "http://localhost:8080/files";
    }

    @Data
    public static class Minio {
        /**
         * MinIO 服务地址
         */
        private String endpoint = "http://localhost:9000";

        /**
         * 访问密钥
         */
        private String accessKey = "minioadmin";

        /**
         * 密钥
         */
        private String secretKey = "minioadmin";

        /**
         * 存储桶名称
         */
        private String bucket = "vision";
    }
}
