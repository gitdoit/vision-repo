package com.vision.capture.service;

import com.vision.camera.entity.Camera;
import com.vision.camera.mapper.CameraMapper;
import com.vision.capture.dto.CaptureMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 抓图任务服务
 *
 * 职责:
 * - 异步执行单个摄像头的抓图任务
 * - 调用推理管道编排完整流程
 * - 更新摄像头最后抓图时间
 * - 异常处理和日志记录
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CaptureTaskService {

    private final CameraMapper cameraMapper;
    private final InferencePipeline inferencePipeline;

    /**
     * 异步执行抓图任务
     *
     * @param message 抓图任务消息
     */
    @Async("captureExecutor")
    public void executeAsync(CaptureMessage message) {
        String cameraId = message.getCameraId();
        String cameraName = message.getCameraName();

        log.info("开始执行抓图任务: cameraId={}, cameraName={}", cameraId, cameraName);

        try {
            // 查询摄像头最新状态
            Camera camera = cameraMapper.selectById(cameraId);
            if (camera == null) {
                log.warn("摄像头不存在: cameraId={}", cameraId);
                return;
            }

            // 检查摄像头状态
            if (!Boolean.TRUE.equals(camera.getAiEnabled()) || !"online".equals(camera.getStatus())) {
                log.debug("摄像头未启用 AI 或不在线，跳过抓图: cameraId={}, aiEnabled={}, status={}",
                        cameraId, camera.getAiEnabled(), camera.getStatus());
                return;
            }

            // 执行推理管道
            inferencePipeline.execute(camera);

            // 更新最后抓图时间
            updateLastCaptureTime(cameraId);

            log.info("抓图任务执行成功: cameraId={}, cameraName={}", cameraId, cameraName);

        } catch (Exception e) {
            log.error("抓图任务执行异常: cameraId={}, cameraName={}", cameraId, cameraName, e);
            // 不抛出异常，避免影响其他任务
        }
    }

    /**
     * 更新摄像头最后抓图时间
     */
    private void updateLastCaptureTime(String cameraId) {
        try {
            cameraMapper.updateLastCaptureTime(cameraId, LocalDateTime.now());
        } catch (Exception e) {
            log.error("更新摄像头最后抓图时间失败: cameraId={}", cameraId, e);
        }
    }
}
