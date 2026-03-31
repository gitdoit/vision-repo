package com.vision.task.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vision.alert.entity.Alert;
import com.vision.alert.service.AlertPushService;
import com.vision.camera.entity.Camera;
import com.vision.inference.entity.Detection;
import com.vision.task.dto.TaskCallbackPayload;
import com.vision.task.entity.MonitorTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 任务告警回调推送服务
 *
 * 职责:
 * - 根据任务配置的 pushMethods 决定推送渠道
 * - HTTP 回调: POST 到 callbackUrl，超时 10s，失败重试 2 次
 * - WebSocket: 复用 AlertPushService 推送到前端
 */
@Slf4j
@Service
public class TaskCallbackService {

    private final AlertPushService alertPushService;
    private final ObjectMapper objectMapper;
    private final RestTemplate callbackRestTemplate;

    public TaskCallbackService(AlertPushService alertPushService, ObjectMapper objectMapper) {
        this.alertPushService = alertPushService;
        this.objectMapper = objectMapper;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);
        this.callbackRestTemplate = new RestTemplate(factory);
    }

    /**
     * 异步推送告警
     */
    @Async("captureExecutor")
    public void pushAlert(MonitorTask task, Alert alert, Camera camera,
                          List<Detection> matchedDetections, String modelName,
                          Integer inferenceTimeMs, String imageUrl) {

        String pushMethods = task.getPushMethods();
        if (pushMethods == null || pushMethods.isBlank()) {
            return;
        }

        // WebSocket 推送
        if (pushMethods.contains("websocket")) {
            try {
                alertPushService.pushAlert(alert);
            } catch (Exception e) {
                log.error("WebSocket推送告警失败: taskId={}, alertId={}", task.getId(), alert.getId(), e);
            }
        }

        // HTTP 回调推送
        if (pushMethods.contains("http_callback") && task.getCallbackUrl() != null && !task.getCallbackUrl().isBlank()) {
            TaskCallbackPayload payload = buildPayload(task, alert, camera, matchedDetections, modelName, inferenceTimeMs, imageUrl);
            httpCallback(task, payload);
        }
    }

    /**
     * 构建回调报文
     */
    private TaskCallbackPayload buildPayload(MonitorTask task, Alert alert, Camera camera,
                                             List<Detection> detections, String modelName,
                                             Integer inferenceTimeMs, String imageUrl) {
        TaskCallbackPayload payload = new TaskCallbackPayload();
        payload.setAlertId(alert.getId());
        payload.setAlertLevel(alert.getAlertLevel());
        payload.setAlertType(task.getAlertTarget());
        payload.setAlertTime(alert.getAlertTime());
        payload.setCaptureTime(alert.getCaptureTime());
        payload.setTriggerCondition(alert.getTriggerCondition());

        // 任务信息
        TaskCallbackPayload.TaskInfo taskInfo = new TaskCallbackPayload.TaskInfo();
        taskInfo.setTaskId(task.getId());
        taskInfo.setTaskName(task.getName());
        taskInfo.setBusinessLine(task.getBusinessLine());
        payload.setTask(taskInfo);

        // 摄像头信息
        TaskCallbackPayload.CameraInfo cameraInfo = new TaskCallbackPayload.CameraInfo();
        cameraInfo.setCameraId(camera.getId());
        cameraInfo.setCameraName(camera.getName());
        cameraInfo.setStreamId(camera.getChannelNo());
        cameraInfo.setLocation(camera.getLocation());
        payload.setCamera(cameraInfo);

        // 检测结果
        TaskCallbackPayload.DetectionInfo detectionInfo = new TaskCallbackPayload.DetectionInfo();
        detectionInfo.setModelName(modelName);
        detectionInfo.setInferenceTimeMs(inferenceTimeMs);
        detectionInfo.setTargets(detections.stream().map(d -> {
            TaskCallbackPayload.TargetInfo target = new TaskCallbackPayload.TargetInfo();
            target.setClassName(d.getLabel());
            target.setConfidence(d.getConfidence() != null ? d.getConfidence().doubleValue() : null);
            target.setBbox(d.getBbox());
            return target;
        }).collect(Collectors.toList()));
        payload.setDetection(detectionInfo);

        // 证据
        TaskCallbackPayload.EvidenceInfo evidence = new TaskCallbackPayload.EvidenceInfo();
        evidence.setImageUrl(imageUrl);
        payload.setEvidence(evidence);

        return payload;
    }

    /**
     * HTTP 回调，最多重试 2 次
     */
    private void httpCallback(MonitorTask task, TaskCallbackPayload payload) {
        int maxRetries = 2;

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                // 添加自定义请求头
                if (task.getCallbackHeaders() != null && !task.getCallbackHeaders().isBlank()) {
                    try {
                        Map<String, String> customHeaders = objectMapper.readValue(
                                task.getCallbackHeaders(), new TypeReference<>() {});
                        customHeaders.forEach(headers::set);
                    } catch (Exception e) {
                        log.warn("解析自定义请求头失败: taskId={}", task.getId(), e);
                    }
                }

                HttpEntity<TaskCallbackPayload> request = new HttpEntity<>(payload, headers);
                ResponseEntity<String> response = callbackRestTemplate.exchange(
                        task.getCallbackUrl(), HttpMethod.POST, request, String.class);

                if (response.getStatusCode().is2xxSuccessful()) {
                    log.info("HTTP回调成功: taskId={}, alertId={}, url={}", task.getId(), payload.getAlertId(), task.getCallbackUrl());
                    return;
                }

                log.warn("HTTP回调返回非2xx: taskId={}, status={}, attempt={}/{}", task.getId(), response.getStatusCode(), attempt, maxRetries);

            } catch (Exception e) {
                log.warn("HTTP回调失败: taskId={}, url={}, attempt={}/{}, error={}",
                        task.getId(), task.getCallbackUrl(), attempt, maxRetries, e.getMessage());
            }

            // 重试前等待
            if (attempt < maxRetries) {
                try {
                    Thread.sleep(1000L * (attempt + 1));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }

        log.error("HTTP回调最终失败: taskId={}, alertId={}, url={}", task.getId(), payload.getAlertId(), task.getCallbackUrl());
    }
}
