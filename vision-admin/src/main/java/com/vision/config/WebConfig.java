package com.vision.config;

import com.vision.storage.StorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 配置
 * 配置 CORS 和静态资源映射
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final StorageProperties storageProperties;

    /**
     * CORS 跨域配置
     * 开发阶段允许所有来源，生产环境建议限制
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    /**
     * 静态资源映射
     * 将 /files/** 映射到本地存储目录，用于访问本地存储的图片文件
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 从 Spring 配置属性中读取存储路径
        String storagePath = storageProperties.getLocal().getBasePath();

        // 将相对路径转为绝对路径
        java.nio.file.Path resolved = java.nio.file.Paths.get(storagePath).toAbsolutePath().normalize();

        registry.addResourceHandler("/files/**")
                .addResourceLocations("file:" + resolved + "/")
                .setCachePeriod(3600); // 缓存1小时
    }
}
