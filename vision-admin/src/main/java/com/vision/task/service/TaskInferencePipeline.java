package com.vision.task.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vision.alert.entity.Alert;
import com.vision.alert.mapper.AlertMapper;
import com.vision.camera.entity.Camera;
import com.vision.camera.mapper.CameraMapper;
import com.vision.common.util.IdUtil;
import com.vision.inference.entity.Detection;
import com.vision.inference.entity.InferenceRecord;
import com.vision.inference.mapper.DetectionMapper;
import com.vision.inference.mapper.InferenceMapper;
import com.vision.model.entity.Model;
import com.vision.model.entity.ModelNodeDeployment;
import com.vision.model.service.InferenceClient;
import com.vision.model.service.ModelService;
import com.vision.node.service.NodeRouter;
import com.vision.task.entity.MonitorTask;
import com.vision.task.mapper.MonitorTaskMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 任务级推理管道
 *
 * 编排: 选节点 → 推理 → 条件评估 → 告警存储 → 回调推送 → 更新统计
 *
 * 截帧和上传已由 CaptureWorker 预先完成，本管道直接消费 CaptureFrame。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskInferencePipeline {

    private final InferenceClient inferenceClient;
    private final InferenceMapper inferenceMapper;
    private final DetectionMapper detectionMapper;
    private final AlertMapper alertMapper;
    private final CameraMapper cameraMapper;
    private final MonitorTaskMapper monitorTaskMapper;
    private final TaskConditionEvaluator conditionEvaluator;
    private final TaskCallbackService callbackService;
    private final NodeRouter nodeRouter;
    private final ModelService modelService;
    private final ObjectMapper objectMapper;

    /**
     * 执行单个摄像头的任务级推理管道（消费预截帧结果）
     *
     * @param task   监测任务
     * @param camera 摄像头
     * @param model  关联模型
     * @param frame  预截帧结果（由 CaptureWorker 产生）
     */
    public void execute(MonitorTask task, Camera camera, Model model, CaptureFrame frame) {
        String taskId = task.getId();
        String cameraId = camera.getId();
        String imageUrl = frame.getImageUrl();

        log.info("▶ 开始任务推理: taskId={}, taskName={}, cameraId={}, modelId={}, frameId={}",
                taskId, task.getName(), cameraId, model.getId(), frame.getFrameId());

        long startTime = System.currentTimeMillis();

        try {
            // 1. 选择推理节点并调用
            long stepStart = System.currentTimeMillis();
            Map<String, Object> inferenceResult = performInference(task, model, imageUrl);
            if (inferenceResult == null) {
                log.warn("✗ 推理失败，终止管道: taskId={}, modelId={}", taskId, model.getId());
                return;
            }
            log.info("  [1/6] 推理完成: 耗时={}ms, 检测数={}",
                    System.currentTimeMillis() - stepStart,
                    inferenceResult.get("objects") instanceof List ? ((List<?>) inferenceResult.get("objects")).size() : 0);

            // 2. 保存推理记录
            InferenceRecord record = saveInferenceRecord(task, camera, model, imageUrl, inferenceResult, frame.getCaptureTimeMs());
            log.info("  [2/6] 推理记录已保存: recordId={}", record.getId());

            // 3. 保存检测结果
            List<Detection> detections = saveDetections(record.getId(), inferenceResult);
            log.info("  [3/6] 检测结果已保存: detectionCount={}", detections.size());

            // 4. 更新任务推理统计
            monitorTaskMapper.incrementInference(taskId, LocalDateTime.now());

            // 5. 条件评估
            List<Detection> matchedDetections = conditionEvaluator.evaluate(detections, task, cameraId, frame.getFrameId());
            log.info("  [4-5/6] 条件评估完成: 匹配检测数={}", matchedDetections.size());

            if (!matchedDetections.isEmpty()) {
                // 6. 创建告警
                Alert alert = createAlert(task, camera, record, matchedDetections);

                // 更新推理记录告警状态
                record.setAlertStatus("alert");
                inferenceMapper.updateById(record);

                // 更新任务告警统计
                monitorTaskMapper.incrementAlert(taskId, LocalDateTime.now());

                // 推送告警
                Integer inferenceTimeMs = record.getInferenceTimeMs();
                callbackService.pushAlert(task, alert, camera, matchedDetections,
                        model.getName(), inferenceTimeMs, imageUrl);

                log.info("任务触发告警: taskId={}, cameraId={}, alertId={}",
                        taskId, cameraId, alert.getId());
            }

            long totalMs = System.currentTimeMillis() - startTime;
            log.info("◼ 任务推理完成: taskId={}, cameraId={}, 总耗时={}ms, 告警={}",
                    taskId, cameraId, totalMs, !matchedDetections.isEmpty() ? "是" : "否");

        } catch (Exception e) {
            log.error("任务推理异常: taskId={}, cameraId={}", taskId, cameraId, e);
        }
    }

    private Map<String, Object> performInference(MonitorTask task, Model model, String imageUrl) {
        try {
            // 选择推理节点
            String nodeId = selectNode(task, model);
            if (nodeId == null) {
                log.warn("无可用推理节点: taskId={}, modelId={}", task.getId(), model.getId());
                return null;
            }

            BigDecimal confidence = task.getAlertConfidence() != null
                    ? task.getAlertConfidence()
                    : model.getConfidenceThreshold();

            return inferenceClient.predict(nodeId, imageUrl, model.getId(), confidence, model.getTaskType());

        } catch (Exception e) {
            log.error("推理失败: taskId={}, modelId={}", task.getId(), model.getId(), e);
            return null;
        }
    }

    /**
     * 选择推理节点：优先使用任务指定节点，否则使用模型所在节点
     */
    private String selectNode(MonitorTask task, Model model) {
        // 任务指定节点
        if (task.getNodeIds() != null && !task.getNodeIds().isBlank()) {
            List<String> specifiedNodes = Arrays.stream(task.getNodeIds().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();

            // 从指定节点中选择一个可用的
            for (String nodeId : specifiedNodes) {
                try {
                    String url = nodeRouter.getNodeUrl(nodeId);
                    if (url != null) {
                        return nodeId;
                    }
                } catch (Exception e) {
                    log.debug("指定节点不可用: nodeId={}", nodeId);
                }
            }
            log.warn("所有指定节点均不可用: taskId={}", task.getId());
        }

        // 使用模型已部署的节点
        ModelNodeDeployment deployment = modelService.findLoadedDeployment(model.getId());
        if (deployment != null) {
            return deployment.getNodeId();
        }

        return null;
    }

    private InferenceRecord saveInferenceRecord(MonitorTask task, Camera camera, Model model,
                                                String imageUrl, Map<String, Object> result, int captureTimeMs) {
        InferenceRecord record = new InferenceRecord();
        record.setId(IdUtil.uuid());
        record.setTaskId(task.getId());
        record.setCameraId(camera.getId());
        record.setBusinessType(task.getBusinessLine());
        record.setThumbnailUrl(imageUrl);
        record.setOriginalImageUrl(imageUrl);
        record.setModelName(model.getName());
        try {
            record.setRawJson(objectMapper.writeValueAsString(result));
        } catch (Exception e) {
            record.setRawJson("{}");
            log.warn("序列化推理结果失败", e);
        }
        record.setAlertStatus("normal");
        record.setCreatedAt(LocalDateTime.now());
        record.setCaptureTimeMs(captureTimeMs);

        if (result.get("inference_time_ms") != null) {
            record.setInferenceTimeMs(((Number) result.get("inference_time_ms")).intValue());
        }

        List<Map<String, Object>> objects = (List<Map<String, Object>>) result.get("objects");
        if (objects != null && !objects.isEmpty()) {
            double avg = objects.stream()
                    .mapToDouble(o -> {
                        Object conf = o.get("confidence");
                        return conf instanceof Number ? ((Number) conf).doubleValue() : 0.0;
                    })
                    .average().orElse(0.0);
            record.setAvgConfidence(BigDecimal.valueOf(avg));
        }

        inferenceMapper.insert(record);
        return record;
    }

    private List<Detection> saveDetections(String recordId, Map<String, Object> inferenceResult) {
        List<Map<String, Object>> objects = (List<Map<String, Object>>) inferenceResult.get("objects");
        if (objects == null || objects.isEmpty()) {
            return List.of();
        }

        return objects.stream().map(obj -> {
            Detection detection = new Detection();
            detection.setId(IdUtil.uuid());
            detection.setRecordId(recordId);
            detection.setLabel((String) obj.get("label"));

            Object conf = obj.get("confidence");
            if (conf instanceof Number) {
                detection.setConfidence(BigDecimal.valueOf(((Number) conf).doubleValue()));
            }

            List<Object> bbox = (List<Object>) obj.get("bbox");
            if (bbox != null && !bbox.isEmpty()) {
                detection.setBbox(bbox.stream().map(Object::toString).collect(Collectors.joining(",")));
            }

            Object count = obj.get("count");
            detection.setCount(count != null ? ((Number) count).intValue() : 1);

            detectionMapper.insert(detection);
            return detection;
        }).toList();
    }

    private Alert createAlert(MonitorTask task, Camera camera, InferenceRecord record,
                              List<Detection> matchedDetections) {
        Alert alert = new Alert();
        alert.setId(IdUtil.uuid());
        alert.setTaskId(task.getId());
        alert.setAlertLevel(task.getAlertLevel());
        alert.setAlertType(task.getAlertTarget());
        alert.setScene(task.getBusinessLine());
        alert.setCameraId(camera.getId());
        alert.setCaptureTime(record.getCreatedAt());
        alert.setAlertTime(LocalDateTime.now());
        alert.setReadStatus(false);

        // 触发条件描述
        String targets = matchedDetections.stream()
                .map(d -> d.getLabel() + "(conf=" + d.getConfidence() + ")")
                .collect(Collectors.joining(", "));
        alert.setTriggerCondition("任务[" + task.getName() + "] 检测到: " + targets);

        alertMapper.insert(alert);
        return alert;
    }
}
