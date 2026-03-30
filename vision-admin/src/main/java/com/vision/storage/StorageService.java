package com.vision.storage;

import java.io.InputStream;

/**
 * 存储服务接口
 * 所有文件操作通过此接口，运行时按配置切换实现（本地磁盘或 MinIO）
 */
public interface StorageService {

    /**
     * 上传文件
     *
     * @param inputStream 文件流
     * @param path        存储路径，如 "images/2026-03-28/cam_001_xxx.jpg"
     * @param contentType 内容类型，如 "image/jpeg"
     * @return 可访问的 URL
     */
    String upload(InputStream inputStream, String path, String contentType);

    /**
     * 获取文件访问 URL
     *
     * @param path 文件存储路径
     * @return 访问 URL
     */
    String getUrl(String path);

    /**
     * 删除文件
     *
     * @param path 文件存储路径
     */
    void delete(String path);

    /**
     * 检查文件是否存在
     *
     * @param path 文件存储路径
     * @return 是否存在
     */
    boolean exists(String path);

    /**
     * 将访问 URL 转换为本地文件系统绝对路径。
     * 用于将存储在数据库中的 URL 还原为推理服务可访问的文件路径。
     *
     * @param url 文件的访问 URL（由 upload / getUrl 返回）
     * @return 本地文件系统绝对路径
     */
    String resolveToFilePath(String url);
}
