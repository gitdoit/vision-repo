package com.vision.config;

import com.vision.storage.LocalStorageService;
import com.vision.storage.MinioStorageService;
import com.vision.storage.StorageProperties;
import com.vision.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 存储配置
 * 根据 vision.storage.type 配置自动注入对应的存储服务实现
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(StorageProperties.class)
public class StorageConfig {

    private final StorageProperties storageProperties;

    /**
     * 本地存储实现
     * 当 vision.storage.type=local 时生效
     */
    @Bean
    @ConditionalOnProperty(name = "vision.storage.type", havingValue = "local")
    public StorageService localStorageService() {
        log.info("Initializing local storage service with base path: {}",
                storageProperties.getLocal().getBasePath());
        return new LocalStorageService(storageProperties);
    }

    /**
     * MinIO 存储实现
     * 当 vision.storage.type=minio 时生效
     */
    @Bean
    @ConditionalOnProperty(name = "vision.storage.type", havingValue = "minio")
    public StorageService minioStorageService() {
        log.info("Initializing MinIO storage service with endpoint: {}",
                storageProperties.getMinio().getEndpoint());
        return new MinioStorageService(storageProperties);
    }
}
