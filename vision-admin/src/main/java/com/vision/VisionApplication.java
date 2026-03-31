package com.vision;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Vision Analysis Platform - Main Application
 *
 * 视觉分析平台主入口
 *
 * 启用功能:
 * - @EnableScheduling: 定时任务调度（用于抓图任务）
 * - @EnableAsync: 异步任务支持
 * - @EnableCaching: 缓存支持（Caffeine）
 */
@SpringBootApplication
@EnableScheduling
@EnableAsync
@EnableCaching
@MapperScan("com.vision.*.mapper")
public class VisionApplication {

    public static void main(String[] args) {
        SpringApplication.run(VisionApplication.class, args);
        System.out.println("""

                ========================================
                   Vision Analysis Platform Started
                   API Doc: http://localhost:26330/swagger-ui.html
                ========================================
                """);
    }
}
