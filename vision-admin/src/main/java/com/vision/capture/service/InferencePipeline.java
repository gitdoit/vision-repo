package com.vision.capture.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vision.alert.entity.Alert;
import com.vision.alert.mapper.AlertMapper;
import com.vision.alert.service.AlertPushService;
import com.vision.camera.entity.Camera;
import com.vision.common.util.FFmpegUtil;
import com.vision.common.util.IdUtil;
import com.vision.inference.entity.Detection;
import com.vision.inference.entity.InferenceRecord;
import com.vision.inference.mapper.DetectionMapper;
import com.vision.inference.mapper.InferenceMapper;
import com.vision.model.entity.Model;
import com.vision.model.entity.ModelNodeDeployment;
import com.vision.model.mapper.ModelMapper;
import com.vision.model.service.InferenceClient;
import com.vision.model.service.ModelService;
import com.vision.node.service.NodeRouter;
import com.vision.rule.entity.Rule;
import com.vision.rule.entity.RuleCondition;
import com.vision.rule.mapper.RuleConditionMapper;
import com.vision.rule.mapper.RuleMapper;
import com.vision.rule.service.RuleEvaluator;
import com.vision.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 推理管道 - 编排完整链路
 *
 * 职责:
 * - 编排: 抓帧 → 存储 → 推理 → 规则评估 → 告警
 * - 协调各模块服务
 * - 异常处理和回滚
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InferencePipeline {

    private final StorageService storageService;
    private final InferenceClient inferenceClient;
    private final InferenceMapper inferenceMapper;
    private final DetectionMapper detectionMapper;
    private final RuleMapper ruleMapper;
    private final RuleConditionMapper ruleConditionMapper;
    private final AlertMapper alertMapper;
    private final AlertPushService alertPushService;
    private final ModelMapper modelMapper;
    private final ModelService modelService;
    private final ObjectMapper objectMapper;

    @Value("${vision.capture.ffmpeg-path:ffmpeg}")
    private String ffmpegPath;

    @Value("${vision.capture.temp-dir:/tmp/vision-capture}")
    private String tempDir;

    /**
     * 执行完整推理管道
     *
     * @param camera 摄像头
     */
    public void execute(Camera camera) {
        String cameraId = camera.getId();
        String cameraName = camera.getName();

        log.info("开始执行推理管道: cameraId={}, cameraName={}", cameraId, cameraName);

        // 临时文件路径
        Path tempFile = null;

        try {
            // 1. 抓帧
            tempFile = captureFrame(camera);
            if (tempFile == null) {
                log.warn("抓帧失败，终止管道: cameraId={}", cameraId);
                return;
            }

            // 2. 上传图片到存储
            String imageUrl = uploadImage(tempFile, cameraId);
            if (imageUrl == null) {
                log.warn("上传图片失败，终止管道: cameraId={}", cameraId);
                return;
            }

            // 3. 调用推理服务
            Map<String, Object> inferenceResult = performInference(imageUrl, camera);
            if (inferenceResult == null) {
                log.warn("推理失败，终止管道: cameraId={}", cameraId);
                return;
            }

            // 4. 保存推理记录
            InferenceRecord record = saveInferenceRecord(camera, imageUrl, inferenceResult);

            // 5. 保存检测结果
            List<Detection> detections = saveDetections(record.getId(), inferenceResult);

            // 6. 规则评估和告警创建
            evaluateRulesAndCreateAlert(camera, detections, record);

            log.info("推理管道执行完成: cameraId={}, inferenceId={}", cameraId, record.getId());

        } catch (Exception e) {
            log.error("推理管道执行异常: cameraId={}", cameraId, e);
        } finally {
            // 清理临时文件
            cleanupTempFile(tempFile);
        }
    }

    /**
     * 1. 抓帧
     */
    private Path captureFrame(Camera camera) {
        try {
            String fileName = "capture_" + camera.getId() + "_" + System.currentTimeMillis() + ".jpg";
            Path outputPath = Path.of(tempDir, fileName);

            boolean success = FFmpegUtil.captureFrame(
                    ffmpegPath,
                    camera.getStreamUrl(),
                    outputPath.toString()
            );

            if (success) {
                log.debug("抓帧成功: cameraId={}, outputPath={}", camera.getId(), outputPath);
                return outputPath;
            } else {
                log.warn("抓帧失败: cameraId={}, streamUrl={}", camera.getId(), camera.getStreamUrl());
                return null;
            }

        } catch (Exception e) {
            log.error("抓帧异常: cameraId={}", camera.getId(), e);
            return null;
        }
    }

    /**
     * 2. 上传图片到存储
     */
    private String uploadImage(Path imageFile, String cameraId) {
        try {
            // 生成存储路径
            String datePath = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String fileName = "camera_" + cameraId + "_" + IdUtil.uuid() + ".jpg";
            String path = "captures/" + datePath + "/" + fileName;

            // 上传
            try (FileInputStream fis = new FileInputStream(imageFile.toFile())) {
                String url = storageService.upload(fis, path, "image/jpeg");
                log.debug("图片上传成功: cameraId={}, path={}, url={}", cameraId, path, url);
                return url;
            }

        } catch (Exception e) {
            log.error("上传图片异常: cameraId={}", cameraId, e);
            return null;
        }
    }

    /**
     * 3. 调用推理服务
     */
    private Map<String, Object> performInference(String imageUrl, Camera camera) {
        try {
            // 查找第一个有 loaded 部署的模型
            List<Model> models = modelMapper.selectList(null);
            ModelNodeDeployment deployment = null;
            Model loadedModel = null;
            for (Model m : models) {
                deployment = modelService.findLoadedDeployment(m.getId());
                if (deployment != null) {
                    loadedModel = m;
                    break;
                }
            }

            if (loadedModel == null || deployment == null) {
                log.warn("没有已加载的模型，无法推理: cameraId={}", camera.getId());
                return null;
            }

            Map<String, Object> result = inferenceClient.predict(
                    deployment.getNodeId(), imageUrl, loadedModel.getId(), BigDecimal.valueOf(0.5));

            log.debug("推理完成: cameraId={}, result={}", camera.getId(), result);
            return result;

        } catch (Exception e) {
            log.error("推理异常: cameraId={}", camera.getId(), e);
            return null;
        }
    }

    /**
     * 4. 保存推理记录
     */
    private InferenceRecord saveInferenceRecord(Camera camera, String imageUrl, Map<String, Object> inferenceResult) {
        InferenceRecord record = new InferenceRecord();
        record.setId(IdUtil.uuid());
        record.setCameraId(camera.getId());
        record.setBusinessType(camera.getBusinessLine());
        record.setThumbnailUrl(imageUrl);
        record.setOriginalImageUrl(imageUrl);
        try {
            record.setRawJson(objectMapper.writeValueAsString(inferenceResult));
        } catch (Exception e) {
            record.setRawJson("{}");
        }
        record.setCreatedAt(LocalDateTime.now());

        // 解析推理结果
        if (inferenceResult.get("inference_time_ms") != null) {
            record.setInferenceTimeMs(((Number) inferenceResult.get("inference_time_ms")).intValue());
        }

        // 计算平均置信度
        List<Map<String, Object>> objects = (List<Map<String, Object>>) inferenceResult.get("objects");
        if (objects != null && !objects.isEmpty()) {
            double avgConfidence = objects.stream()
                    .mapToDouble(o -> {
                        Object conf = o.get("confidence");
                        if (conf instanceof Number) {
                            return ((Number) conf).doubleValue();
                        }
                        return 0.0;
                    })
                    .average()
                    .orElse(0.0);
            record.setAvgConfidence(BigDecimal.valueOf(avgConfidence));
        }

        inferenceMapper.insert(record);
        return record;
    }

    /**
     * 5. 保存检测结果
     */
    private List<Detection> saveDetections(String recordId, Map<String, Object> inferenceResult) {
        List<Map<String, Object>> objects = (List<Map<String, Object>>) inferenceResult.get("objects");
        if (objects == null || objects.isEmpty()) {
            return List.of();
        }

        for (Map<String, Object> obj : objects) {
            Detection detection = new Detection();
            detection.setId(IdUtil.uuid());
            detection.setRecordId(recordId);
            detection.setLabel((String) obj.get("label"));

            Object conf = obj.get("confidence");
            if (conf instanceof Number) {
                detection.setConfidence(BigDecimal.valueOf(((Number) conf).doubleValue()));
            }

            detection.setBbox(formatBbox((List<Object>) obj.get("bbox")));

            Object count = obj.get("count");
            detection.setCount(count != null ? ((Number) count).intValue() : 1);

            detectionMapper.insert(detection);
        }

        // 返回 Detection 列表用于规则评估
        return objects.stream()
                .map(obj -> {
                    Detection d = new Detection();
                    d.setLabel((String) obj.get("label"));
                    Object conf = obj.get("confidence");
                    if (conf instanceof Number) {
                        d.setConfidence(BigDecimal.valueOf(((Number) conf).doubleValue()));
                    }
                    d.setBbox(formatBbox((List<Object>) obj.get("bbox")));
                    Object count = obj.get("count");
                    d.setCount(count != null ? ((Number) count).intValue() : 1);
                    return d;
                })
                .toList();
    }

    /**
     * 6. 规则评估和告警创建
     */
    private void evaluateRulesAndCreateAlert(Camera camera, List<Detection> detections, InferenceRecord record) {
        // 查询所有启用的规则
        List<Rule> enabledRules = ruleMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Rule>()
                        .eq(Rule::getEnabled, true)
        );

        for (Rule rule : enabledRules) {
            try {
                // 查询规则条件
                List<RuleCondition> conditions = ruleConditionMapper.selectList(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<RuleCondition>()
                                .eq(RuleCondition::getRuleId, rule.getId())
                );

                // 评估规则
                boolean hit = RuleEvaluator.evaluate(detections, conditions);

                if (hit) {
                    // 创建告警
                    Alert alert = new Alert();
                    alert.setId(IdUtil.uuid());
                    alert.setAlertLevel(rule.getPriority());
                    alert.setAlertType("rule_trigger");
                    alert.setScene(camera.getBusinessLine());
                    alert.setCameraId(camera.getId());
                    alert.setCaptureTime(record.getCreatedAt());
                    alert.setAlertTime(LocalDateTime.now());
                    alert.setTriggerCondition("规则触发: " + rule.getName());
                    alert.setRuleId(rule.getId());
                    alert.setReadStatus(false);

                    // 保存告警
                    alertMapper.insert(alert);

                    // 实时推送
                    alertPushService.pushAlert(alert);

                    log.info("触发告警: cameraId={}, ruleId={}, alertLevel={}",
                            camera.getId(), rule.getId(), rule.getPriority());
                }

            } catch (Exception e) {
                log.error("规则评估异常: ruleId={}, cameraId={}", rule.getId(), camera.getId(), e);
            }
        }
    }

    /**
     * 格式化 bbox 为字符串
     */
    private String formatBbox(List<Object> bbox) {
        if (bbox == null || bbox.isEmpty()) {
            return null;
        }
        return bbox.stream()
                .map(Object::toString)
                .reduce((a, b) -> a + "," + b)
                .orElse(null);
    }

    /**
     * 清理临时文件
     */
    private void cleanupTempFile(Path tempFile) {
        if (tempFile != null && Files.exists(tempFile)) {
            try {
                Files.delete(tempFile);
                log.debug("临时文件已删除: {}", tempFile);
            } catch (Exception e) {
                log.warn("删除临时文件失败: {}", tempFile, e);
            }
        }
    }
}
