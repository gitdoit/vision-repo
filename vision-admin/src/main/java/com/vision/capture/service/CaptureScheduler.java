package com.vision.capture.service;

import com.vision.camera.entity.Camera;
import com.vision.camera.mapper.CameraMapper;
import com.vision.capture.dto.CaptureMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 抓图调度器
 *
 * 职责:
 * - 定时扫描需要抓图的摄像头
 * - 检查抓图频率是否到期
 * - 提交异步任务执行
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CaptureScheduler {

    private final CameraMapper cameraMapper;
    private final CaptureTaskService captureTaskService;

    /**
     * 定时扫描活跃摄像头
     * 每 60 秒执行一次
     */
    @Scheduled(fixedDelay = 60000, initialDelay = 10000)
    public void scheduleCaptureTasks() {
        try {
            log.debug("开始扫描待抓图摄像头...");

            // 查询所有启用 AI 且在线的摄像头
            List<Camera> activeCameras = cameraMapper.selectActiveCameras();

            if (activeCameras.isEmpty()) {
                log.debug("无活跃摄像头需要抓图");
                return;
            }

            LocalDateTime now = LocalDateTime.now();

            for (Camera camera : activeCameras) {
                try {
                    // 检查是否需要抓图
                    if (shouldCapture(camera, now)) {
                        submitCaptureTask(camera);
                    }
                } catch (Exception e) {
                    log.error("处理摄像头抓图任务异常: cameraId={}", camera.getId(), e);
                }
            }

            log.debug("抓图任务扫描完成");

        } catch (Exception e) {
            log.error("抓图调度异常", e);
        }
    }

    /**
     * 判断是否需要抓图
     */
    private boolean shouldCapture(Camera camera, LocalDateTime now) {
        // 检查上次抓图时间
        LocalDateTime lastCaptureTime = camera.getLastCaptureTime();

        if (lastCaptureTime == null) {
            log.debug("摄像头首次抓图: cameraId={}, cameraName={}", camera.getId(), camera.getName());
            return true;
        }

        // 解析抓图频率
        int frequencySeconds = parseFrequencySeconds(camera.getCaptureFrequency());

        // 计算距离上次抓图的秒数
        long secondsSinceLastCapture = Duration.between(lastCaptureTime, now).getSeconds();

        if (secondsSinceLastCapture >= frequencySeconds) {
            log.debug("摄像头抓图时间到期: cameraId={}, cameraName={}, 上次抓图: {}, 距今: {}秒",
                    camera.getId(), camera.getName(), lastCaptureTime, secondsSinceLastCapture);
            return true;
        }

        return false;
    }

    /**
     * 解析抓图频率字符串为秒数
     *
     * 支持格式:
     * - 1s, 5s, 30s - 秒
     * - 1min, 5min - 分钟
     * - 1h, 2h - 小时
     */
    private int parseFrequencySeconds(String frequency) {
        if (frequency == null || frequency.isBlank()) {
            return 300; // 默认 5 分钟
        }

        frequency = frequency.toLowerCase().trim();

        try {
            // 纯数字，视为秒
            return Integer.parseInt(frequency);
        } catch (NumberFormatException e) {
            // 忽略，继续处理带单位的格式
        }

        // 处理带单位的格式
        if (frequency.endsWith("s")) {
            return Integer.parseInt(frequency.replace("s", "").trim());
        } else if (frequency.endsWith("sec")) {
            return Integer.parseInt(frequency.replace("sec", "").trim());
        } else if (frequency.endsWith("min")) {
            return Integer.parseInt(frequency.replace("min", "").trim()) * 60;
        } else if (frequency.endsWith("h")) {
            return Integer.parseInt(frequency.replace("h", "").trim()) * 3600;
        }

        // 默认 5 分钟
        log.warn("无法解析抓图频率: {}, 使用默认值 5 分钟", frequency);
        return 300;
    }

    /**
     * 提交抓图任务
     */
    private void submitCaptureTask(Camera camera) {
        CaptureMessage message = new CaptureMessage(
                camera.getId(),
                camera.getName(),
                camera.getStreamUrl(),
                parseFrequencySeconds(camera.getCaptureFrequency())
        );

        captureTaskService.executeAsync(message);

        log.debug("已提交抓图任务: cameraId={}, cameraName={}", camera.getId(), camera.getName());
    }
}
