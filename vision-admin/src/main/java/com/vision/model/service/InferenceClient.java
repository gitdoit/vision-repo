package com.vision.model.service;

import com.vision.common.exception.BizException;
import com.vision.node.service.NodeRouter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 推理服务HTTP客户端
 * 负责与Python推理服务通信，通过 NodeRouter 路由到目标节点
 */
@Slf4j
@Component
public class InferenceClient {

    private final RestTemplate restTemplate;
    private final NodeRouter nodeRouter;

    public InferenceClient(NodeRouter nodeRouter) {
        this.restTemplate = new RestTemplate();
        this.nodeRouter = nodeRouter;
    }

    /**
     * 上传模型文件到推理节点
     *
     * @return 推理节点上的本地路径
     */
    public String uploadModelFile(String nodeId, byte[] fileBytes, String filename) {
        String baseUrl = nodeRouter.getNodeUrl(nodeId);
        String url = baseUrl + "/models/upload";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(fileBytes) {
            @Override
            public String getFilename() {
                return filename;
            }
        });

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, requestEntity, Map.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null
                    && Boolean.TRUE.equals(response.getBody().get("success"))) {
                return (String) response.getBody().get("local_path");
            }
            throw new BizException("模型文件上传失败");
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("上传模型文件失败: nodeId={}, filename={}", nodeId, filename, e);
            throw new BizException("模型文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 加载模型
     */
    public void loadModel(String nodeId, String modelId, String modelPath, String device) {
        String baseUrl = nodeRouter.getNodeUrl(nodeId);
        String url = baseUrl + "/models/load";

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
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("加载模型失败: nodeId={}, modelId={}", nodeId, modelId, e);
            throw new BizException("模型加载失败: " + e.getMessage());
        }
    }

    /**
     * 卸载模型
     */
    public void unloadModel(String nodeId, String modelId) {
        String baseUrl = nodeRouter.getNodeUrl(nodeId);
        String url = baseUrl + "/models/unload";

        Map<String, Object> request = new HashMap<>();
        request.put("model_id", modelId);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            if (response.getStatusCode() != HttpStatus.OK ||
                !Boolean.TRUE.equals(response.getBody().get("success"))) {
                throw new BizException("模型卸载失败");
            }
        } catch (org.springframework.web.client.HttpClientErrorException.NotFound e) {
            log.warn("推理服务中模型已不存在，视为卸载成功: modelId={}", modelId);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("卸载模型失败: nodeId={}, modelId={}", nodeId, modelId, e);
            throw new BizException("模型卸载失败: " + e.getMessage());
        }
    }

    /**
     * 获取已加载模型列表
     */
    public List<Map<String, Object>> getModelsStatus(String nodeId) {
        String baseUrl = nodeRouter.getNodeUrl(nodeId);
        String url = baseUrl + "/models/status";

        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return (List<Map<String, Object>>) response.getBody().get("models");
            }
        } catch (Exception e) {
            log.error("获取模型状态失败: nodeId={}", nodeId, e);
        }
        return List.of();
    }

    /**
     * 获取推理服务设备信息（CPU/GPU）
     */
    public Map<String, Object> getDeviceInfo(String nodeId) {
        String baseUrl = nodeRouter.getNodeUrl(nodeId);
        String url = baseUrl + "/device/info";

        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody();
            }
        } catch (Exception e) {
            log.error("获取设备信息失败: nodeId={}", nodeId, e);
        }
        Map<String, Object> fallback = new HashMap<>();
        fallback.put("devices", List.of("cpu"));
        fallback.put("cuda_available", false);
        fallback.put("gpu_name", null);
        return fallback;
    }

    /**
     * 发送推理请求
     */
    public Map<String, Object> predict(String nodeId, String imageUrl, String modelId, BigDecimal confidenceThreshold) {
        return predict(nodeId, imageUrl, modelId, confidenceThreshold, null);
    }

    /**
     * 发送推理请求（指定任务类型）
     */
    public Map<String, Object> predict(String nodeId, String imageUrl, String modelId, BigDecimal confidenceThreshold, String taskType) {
        String baseUrl = nodeRouter.getNodeUrl(nodeId);
        String url = baseUrl + "/predict";

        Map<String, Object> request = new HashMap<>();
        request.put("image_url", imageUrl);
        request.put("model_id", modelId);
        request.put("confidence_threshold", confidenceThreshold);
        if (taskType != null && !taskType.isEmpty()) {
            request.put("task_type", taskType);
        }

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody();
            } else {
                throw new BizException("推理请求失败");
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("推理请求失败: nodeId={}, imageUrl={}, modelId={}", nodeId, imageUrl, modelId, e);
            throw new BizException("推理请求失败: " + e.getMessage());
        }
    }

    /**
     * 启动流式推理
     */
    public void startStream(String nodeId, String taskId, String streamUrl, String modelId, int fps, String callbackUrl) {
        String baseUrl = nodeRouter.getNodeUrl(nodeId);
        String url = baseUrl + "/stream/start";

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
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("启动流式推理失败: nodeId={}, taskId={}", nodeId, taskId, e);
            throw new BizException("启动流式推理失败: " + e.getMessage());
        }
    }

    /**
     * 停止流式推理
     */
    public void stopStream(String nodeId, String taskId) {
        String baseUrl = nodeRouter.getNodeUrl(nodeId);
        String url = baseUrl + "/stream/stop";

        Map<String, Object> request = new HashMap<>();
        request.put("task_id", taskId);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            if (response.getStatusCode() != HttpStatus.OK ||
                !Boolean.TRUE.equals(response.getBody().get("success"))) {
                throw new BizException("停止流式推理失败");
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("停止流式推理失败: nodeId={}, taskId={}", nodeId, taskId, e);
            throw new BizException("停止流式推理失败: " + e.getMessage());
        }
    }

    /**
     * 重载推理节点（使新代码生效）
     */
    public Map<String, Object> reloadNode(String nodeId) {
        String baseUrl = nodeRouter.getNodeUrl(nodeId);
        String url = baseUrl + "/admin/reload";

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, null, Map.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody();
            }
            throw new BizException("节点重载失败");
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("节点重载失败: nodeId={}", nodeId, e);
            throw new BizException("节点重载失败: " + e.getMessage());
        }
    }

    /**
     * 健康检查
     */
    public boolean healthCheck(String nodeId) {
        try {
            String baseUrl = nodeRouter.getNodeUrl(nodeId);
            String url = baseUrl + "/health";
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            return response.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            return false;
        }
    }
}
