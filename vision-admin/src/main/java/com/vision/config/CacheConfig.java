package com.vision.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Caffeine 缓存配置
 * 使用本地缓存替代 Redis，降低部署复杂度
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * 缓存管理器
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
                // 初始容量
                .initialCapacity(100)
                // 最大容量
                .maximumSize(1000)
                // 写入后过期时间（10分钟）
                .expireAfterWrite(10, TimeUnit.MINUTES)
                // 统计信息
                .recordStats());
        return cacheManager;
    }

    /**
     * 摄像头缓存配置（单独的 Cache 实例）
     */
    @Bean
    public Caffeine<Object, Object> cameraCaffeine() {
        return Caffeine.newBuilder()
                .initialCapacity(50)
                .maximumSize(200)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .recordStats();
    }

    /**
     * 模型缓存配置
     */
    @Bean
    public Caffeine<Object, Object> modelCaffeine() {
        return Caffeine.newBuilder()
                .initialCapacity(20)
                .maximumSize(100)
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .recordStats();
    }

    /**
     * 规则缓存配置
     */
    @Bean
    public Caffeine<Object, Object> ruleCaffeine() {
        return Caffeine.newBuilder()
                .initialCapacity(50)
                .maximumSize(200)
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .recordStats();
    }
}
