package com.vision.storage;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * 本地磁盘存储服务实现
 * 将文件存储到本地文件系统
 */
@Slf4j
public class LocalStorageService implements StorageService {

    private final StorageProperties properties;
    private final Path basePath;

    public LocalStorageService(StorageProperties properties) {
        this.properties = properties;
        this.basePath = Paths.get(properties.getLocal().getBasePath());

        // 确保基础目录存在
        try {
            Files.createDirectories(basePath);
            log.info("Local storage base directory created/verified: {}", basePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create base directory: " + basePath, e);
        }
    }

    @Override
    public String upload(InputStream inputStream, String path, String contentType) {
        try {
            Path targetPath = basePath.resolve(path);
            Path parentDir = targetPath.getParent();

            // 确保父目录存在
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }

            // 保存文件
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            log.debug("File uploaded to: {}", targetPath);

            return getUrl(path);
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload file: " + path, e);
        }
    }

    @Override
    public String getUrl(String path) {
        String baseUrl = properties.getLocal().getBaseUrl();
        // 确保没有重复的斜杠
        String normalizedPath = path.startsWith("/") ? path.substring(1) : path;
        return baseUrl + "/" + normalizedPath;
    }

    @Override
    public void delete(String path) {
        try {
            Path targetPath = basePath.resolve(path);
            if (Files.exists(targetPath)) {
                Files.delete(targetPath);
                log.debug("File deleted: {}", targetPath);
            }
        } catch (IOException e) {
            log.error("Failed to delete file: {}", path, e);
        }
    }

    @Override
    public boolean exists(String path) {
        Path targetPath = basePath.resolve(path);
        return Files.exists(targetPath);
    }
}
