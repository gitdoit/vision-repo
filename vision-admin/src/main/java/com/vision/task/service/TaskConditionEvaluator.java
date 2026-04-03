package com.vision.task.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.vision.inference.entity.Detection;
import com.vision.task.entity.MonitorTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 任务级条件评估器
 *
 * 简化版条件评估：目标类别 + 置信度阈值 + 连续帧数
 * 连续帧计数使用 Caffeine 缓存，key = taskId:cameraId
 */
@Slf4j
@Component
public class TaskConditionEvaluator {

    /**
     * 连续帧数缓存: key = "taskId:cameraId", value = 连续命中次数
     * 10 分钟无更新自动过期（认为场景已变化）
     */
    private final Cache<String, AtomicInteger> frameCountCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(10))
            .maximumSize(10000)
            .build();

    /**
     * 记录每个 taskId:cameraId 最后评估的 frameId，防止同一物理帧被重复计数
     */
    private final Map<String, String> lastEvaluatedFrameId = new ConcurrentHashMap<>();

    /**
     * 评估推理结果是否满足任务告警条件
     *
     * @param detections 推理检测结果
     * @param task       监测任务配置
     * @param cameraId   摄像头ID
     * @param frameId    帧唯一标识（用于防止同帧重复计数）
     * @return 命中的目标列表，空列表表示未命中
     */
    public List<Detection> evaluate(List<Detection> detections, MonitorTask task, String cameraId, String frameId) {
        // 帧去重: 同一物理帧不重复计数
        String cacheKey = task.getId() + ":" + cameraId;
        if (frameId != null) {
            String lastFrameId = lastEvaluatedFrameId.get(cacheKey);
            if (frameId.equals(lastFrameId)) {
                log.debug("同一帧已评估过，跳过: taskId={}, cameraId={}, frameId={}", task.getId(), cameraId, frameId);
                return List.of();
            }
            lastEvaluatedFrameId.put(cacheKey, frameId);
        }
        if (detections == null || detections.isEmpty()) {
            resetFrameCount(task.getId(), cameraId);
            return List.of();
        }

        // 1. 解析目标类别白名单
        Set<String> targetLabels = parseTargetLabels(task.getAlertTarget());

        // 2. 过滤匹配的检测结果
        BigDecimal confidenceThreshold = task.getAlertConfidence() != null
                ? task.getAlertConfidence()
                : new BigDecimal("0.50");

        List<Detection> matchedDetections = detections.stream()
                .filter(d -> matchesTarget(d, targetLabels))
                .filter(d -> meetsConfidence(d, confidenceThreshold))
                .collect(Collectors.toList());

        if (matchedDetections.isEmpty()) {
            resetFrameCount(task.getId(), cameraId);
            return List.of();
        }

        // 3. 连续帧数判断
        int requiredFrames = task.getAlertFrames() != null ? task.getAlertFrames() : 1;
        if (requiredFrames <= 1) {
            // 不需要连续帧数，直接命中
            return matchedDetections;
        }

        String countKey = task.getId() + ":" + cameraId;
        AtomicInteger count = frameCountCache.get(countKey, k -> new AtomicInteger(0));
        int currentCount = count.incrementAndGet();

        if (currentCount >= requiredFrames) {
            log.debug("连续帧数达标: taskId={}, cameraId={}, count={}/{}", task.getId(), cameraId, currentCount, requiredFrames);
            // 命中后重置计数，避免后续每帧都触发
            count.set(0);
            return matchedDetections;
        }

        log.debug("连续帧数未达标: taskId={}, cameraId={}, count={}/{}", task.getId(), cameraId, currentCount, requiredFrames);
        return List.of();
    }

    /**
     * 解析目标类别列表
     */
    private Set<String> parseTargetLabels(String alertTarget) {
        if (alertTarget == null || alertTarget.isBlank()) {
            return Set.of(); // 空白表示匹配任意目标
        }
        return Arrays.stream(alertTarget.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    /**
     * 目标类别匹配
     */
    private boolean matchesTarget(Detection detection, Set<String> targetLabels) {
        if (targetLabels.isEmpty()) {
            return true; // 未配置目标类别，匹配所有
        }
        return targetLabels.contains(detection.getLabel());
    }

    /**
     * 置信度匹配
     */
    private boolean meetsConfidence(Detection detection, BigDecimal threshold) {
        if (detection.getConfidence() == null) {
            return false;
        }
        return detection.getConfidence().compareTo(threshold) >= 0;
    }

    /**
     * 重置连续帧计数
     */
    private void resetFrameCount(String taskId, String cameraId) {
        String cacheKey = taskId + ":" + cameraId;
        frameCountCache.invalidate(cacheKey);
    }

    /**
     * 清除指定任务的所有帧计数缓存（任务停止时调用）
     */
    public void clearTaskCache(String taskId) {
        frameCountCache.asMap().keySet().removeIf(key -> key.startsWith(taskId + ":"));
        lastEvaluatedFrameId.keySet().removeIf(key -> key.startsWith(taskId + ":"));
    }
}
