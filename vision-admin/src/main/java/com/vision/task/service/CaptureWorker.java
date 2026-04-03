package com.vision.task.service;

import com.vision.camera.entity.Camera;
import com.vision.camera.mapper.CameraMapper;
import com.vision.common.util.FFmpegUtil;
import com.vision.common.util.IdUtil;
import com.vision.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * 预截帧工作器
 *
 * 职责:
 * - 定时扫描需要截帧的摄像头（由 CaptureCoordinator 管理频率表）
 * - 对到期摄像头执行 FFmpeg 截帧 + StorageService 上传
 * - 将截帧结果写入 CaptureCoordinator 帧缓冲
 * - 处理截帧失败退避逻辑（在正常截帧循环中通过分支判断实现）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CaptureWorker {

    private final CaptureCoordinator captureCoordinator;
    private final CameraMapper cameraMapper;
    private final StorageService storageService;

    @Value("${vision.capture.ffmpeg-path:ffmpeg}")
    private String ffmpegPath;

    @Value("${vision.capture.temp-dir:/tmp/vision-capture}")
    private String tempDir;

    /**
     * 定时扫描并执行截帧
     * 每 10 秒扫描一次频率表，对到期的摄像头提交截帧任务
     */
    @Scheduled(fixedDelay = 10000, initialDelay = 10000)
    public void scanAndCapture() {
        Map<String, Integer> frequencies = captureCoordinator.getCaptureFrequencies();
        if (frequencies.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        for (String cameraId : frequencies.keySet()) {
            try {
                // 退避中且未到重试时间 → 跳过
                if (captureCoordinator.shouldSkipCapture(cameraId, now)) {
                    log.debug("摄像头退避中，跳过截帧: cameraId={}", cameraId);
                    continue;
                }

                // 检查是否到期（正常频率到期 或 退避重试窗口到期）
                boolean normalDue = captureCoordinator.isDueForCapture(cameraId, now);
                boolean retryDue = captureCoordinator.isInRetryWindow(cameraId, now);

                if (!normalDue && !retryDue) {
                    continue;
                }

                // 异步执行截帧
                // TODO 没有异步,自调用 不走异步
                captureAsync(cameraId, retryDue);

            } catch (Exception e) {
                log.error("截帧扫描异常: cameraId={}", cameraId, e);
            }
        }
    }

    /**
     * 异步执行单个摄像头截帧
     */
    @Async("captureWorkerExecutor")
    public void captureAsync(String cameraId, boolean isRetry) {
        Camera camera = cameraMapper.selectById(cameraId);
        if (camera == null || !"online".equals(camera.getStatus())) {
            log.debug("摄像头不在线或不存在，跳过截帧: cameraId={}", cameraId);
            if (isRetry) {
                captureCoordinator.escalateBackoff(cameraId);
            } else {
                captureCoordinator.recordFailure(cameraId);
            }
            return;
        }

        Path tempFile = null;
        long startTime = System.currentTimeMillis();

        try {
            // 1. FFmpeg 抓帧
            String fileName = "capture_" + cameraId + "_" + System.currentTimeMillis() + ".jpg";
            Path outputPath = Path.of(tempDir, fileName);

            boolean success = FFmpegUtil.captureFrame(ffmpegPath, camera.getStreamUrl(), outputPath.toString());
            if (!success) {
                handleCaptureFailure(cameraId, isRetry);
                return;
            }
            tempFile = outputPath;

            // 2. 上传到 StorageService
            String datePath = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String uploadFileName = "camera_" + cameraId + "_" + IdUtil.uuid() + ".jpg";
            String path = "captures/" + datePath + "/" + uploadFileName;

            String imageUrl;
            try (FileInputStream fis = new FileInputStream(tempFile.toFile())) {
                imageUrl = storageService.upload(fis, path, "image/jpeg");
            }

            if (imageUrl == null) {
                handleCaptureFailure(cameraId, isRetry);
                return;
            }

            int captureTimeMs = (int) (System.currentTimeMillis() - startTime);

            // 3. 构建 CaptureFrame 并写入缓冲
            CaptureFrame frame = new CaptureFrame();
            frame.setFrameId(IdUtil.uuid());
            frame.setCameraId(cameraId);
            frame.setImageUrl(imageUrl);
            frame.setCaptureTime(LocalDateTime.now());
            frame.setCaptureTimeMs(captureTimeMs);
            frame.setStatus("success");

            captureCoordinator.putFrame(frame);
            captureCoordinator.recordSuccess(cameraId);

            // 4. 更新摄像头最后抓图时间
            cameraMapper.updateLastCaptureTime(cameraId, LocalDateTime.now());

            log.debug("预截帧完成: cameraId={}, 耗时={}ms, url={}", cameraId, captureTimeMs, imageUrl);

        } catch (Exception e) {
            log.error("预截帧异常: cameraId={}", cameraId, e);
            handleCaptureFailure(cameraId, isRetry);
        } finally {
            cleanupTempFile(tempFile);
        }
    }

    private void handleCaptureFailure(String cameraId, boolean isRetry) {
        if (isRetry) {
            captureCoordinator.escalateBackoff(cameraId);
        } else {
            captureCoordinator.recordFailure(cameraId);
        }
    }

    private void cleanupTempFile(Path tempFile) {
        if (tempFile != null && Files.exists(tempFile)) {
            try {
                Files.delete(tempFile);
            } catch (Exception e) {
                log.warn("删除临时文件失败: {}", tempFile, e);
            }
        }
    }
}
