package com.vision.storage;

import io.minio.*;
import io.minio.errors.MinioException;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * MinIO 存储服务实现
 * 将文件存储到 MinIO 对象存储
 */
@Slf4j
public class MinioStorageService implements StorageService {

    private final StorageProperties properties;
    private final MinioClient minioClient;

    public MinioStorageService(StorageProperties properties) {
        this.properties = properties;
        StorageProperties.Minio minioConfig = properties.getMinio();

        this.minioClient = MinioClient.builder()
                .endpoint(minioConfig.getEndpoint())
                .credentials(minioConfig.getAccessKey(), minioConfig.getSecretKey())
                .build();

        // 确保存储桶存在
        ensureBucketExists();
    }

    private void ensureBucketExists() {
        try {
            String bucket = properties.getMinio().getBucket();
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder()
                    .bucket(bucket)
                    .build());

            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder()
                        .bucket(bucket)
                        .build());
                log.info("MinIO bucket created: {}", bucket);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to ensure bucket exists", e);
        }
    }

    @Override
    public String upload(InputStream inputStream, String path, String contentType) {
        try {
            String bucket = properties.getMinio().getBucket();

            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(path)
                    .stream(inputStream, inputStream.available(), -1)
                    .contentType(contentType)
                    .build());

            log.debug("File uploaded to MinIO: {}", path);
            return getUrl(path);
        } catch (IOException | MinioException e) {
            throw new RuntimeException("Failed to upload file to MinIO: " + path, e);
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error uploading to MinIO", e);
        }
    }

    @Override
    public String getUrl(String path) {
        String bucket = properties.getMinio().getBucket();
        String endpoint = properties.getMinio().getEndpoint();
        return endpoint + "/" + bucket + "/" + path;
    }

    @Override
    public void delete(String path) {
        try {
            String bucket = properties.getMinio().getBucket();
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(path)
                    .build());
            log.debug("File deleted from MinIO: {}", path);
        } catch (MinioException | IOException | InvalidKeyException | NoSuchAlgorithmException e) {
            log.error("Failed to delete file from MinIO: {}", path, e);
        } catch (Exception e) {
            log.error("Unexpected error deleting from MinIO: {}", path, e);
        }
    }

    @Override
    public boolean exists(String path) {
        try {
            String bucket = properties.getMinio().getBucket();
            minioClient.statObject(StatObjectArgs.builder()
                    .bucket(bucket)
                    .object(path)
                    .build());
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
