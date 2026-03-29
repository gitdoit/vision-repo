package com.vision.model.service;

import com.vision.common.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 推理服务HTTP客户端
 * 负责与Python推理服务通信
 */
@Slf4j
@Component
public class InferenceClient {

    private final RestTemplate restTemplate;

    @Value("${vision.inference.service-url:http://localhost:5000}")
    private String inferenceServiceUrl;

    @Value("${vision.inference.connect-timeout:5000}")
    private int connectTimeout;

    @Value("${vision.inference.read-timeout:30000}")
    private int readTimeout;

    public InferenceClient() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * 加载模型
     */
    public void loadModel(String modelId, String modelPath, String device) {
        String url = inferenceServiceUrl + "/models/load";

        Map<String, Object> request = new HashMap<>();
        request.put("model_id", modelId);
        request.put("model_path", modelPath);
        request.put("device", device);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            if (response.getStatusCode() != HttpStatus.OK ||
                !Boolean.TRUE.equals(response.getBody().get("success"))) {
                throw new BizException("模型加载失败");
            }
        } catch (Exception e) {
            log.error("加载模型失败: modelId={}", modelId, e);
            throw new BizException("模型加载失败: " + e.getMessage());
        }
    }

    /**
     * 卸载模型
     */
    public void unloadModel(String modelId) {
        String url = inferenceServiceUrl + "/models/unload";

        Map<String, Object> request = new HashMap<>();
        request.put("model_id", modelId);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            if (response.getStatusCode() != HttpStatus.OK ||
                !Boolean.TRUE.equals(response.getBody().get("success"))) {
                throw new BizException("模型卸载失败");
            }
        } catch (Exception e) {
            log.error("卸载模型失败: modelId={}", modelId, e);
            throw new BizException("模型卸载失败: " + e.getMessage());
        }
    }

    /**
     * 获取已加载模型列表
     */
    public List<Map<String, Object>> getModelsStatus() {
        String url = inferenceServiceUrl + "/models/status";

        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return (List<Map<String, Object>>) response.getBody().get("models");
            }
        } catch (Exception e) {
            log.error("获取模型状态失败", e);
        }
        return List.of();
    }

    /**
     * 发送推理请求
     */
    public Map<String, Object> predict(String imageUrl, String modelId, BigDecimal confidenceThreshold) {
        String url = inferenceServiceUrl + "/predict";

        Map<String, Object> request = new HashMap<>();
        request.put("image_url", imageUrl);
        request.put("model_id", modelId);
        request.put("confidence_threshold", confidenceThreshold);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody();
            } else {
                throw new BizException("推理请求失败");
            }
        } catch (Exception e) {
            log.error("推理请求失败: imageUrl={}, modelId={}", imageUrl, modelId, e);
            throw new BizException("推理请求失败: " + e.getMessage());
        }
    }

    /**
     * 启动流式推理
     */
    public void startStream(String taskId, String streamUrl, String modelId, int fps, String callbackUrl) {
        String url = inferenceServiceUrl + "/stream/start";

        Map<String, Object> request = new HashMap<>();
        request.put("task_id", taskId);
        request.put("stream_url", streamUrl);
        request.put("model_id", modelId);
        request.put("fps", fps);
        request.put("callback_url", callbackUrl);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            if (response.getStatusCode() != HttpStatus.OK ||
                !Boolean.TRUE.equals(response.getBody().get("success"))) {
                throw new BizException("启动流式推理失败");
            }
        } catch (Exception e) {
            log.error("启动流式推理失败: taskId={}", taskId, e);
            throw new BizException("启动流式推理失败: " + e.getMessage());
        }
    }

    /**
     * 停止流式推理
     */
    public void stopStream(String taskId) {
        String url = inferenceServiceUrl + "/stream/stop";

        Map<String, Object> request = new HashMap<>();
        request.put("task_id", taskId);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            if (response.getStatusCode() != HttpStatus.OK ||
                !Boolean.TRUE.equals(response.getBody().get("success"))) {
                throw new BizException("停止流式推理失败");
            }
        } catch (Exception e) {
            log.error("停止流式推理失败: taskId={}", taskId, e);
            throw new BizException("停止流式推理失败: " + e.getMessage());
        }
    }

    /**
     * 健康检查
     */
    public boolean healthCheck() {
        try {
            String url = inferenceServiceUrl + "/health";
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            return response.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            return false;
        }
    }
}
