package com.vision.rule.service;

import com.vision.inference.entity.Detection;
import com.vision.rule.entity.RuleCondition;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 规则评估器
 * 纯Java实现，支持四种条件类型: target, confidence, frames, zone
 */
@Slf4j
public class RuleEvaluator {

    /**
     * 连续帧数缓存 (cameraId -> (label -> (count, lastTime)))
     */
    private static final Map<String, Map<String, FrameInfo>> frameCache = new ConcurrentHashMap<>();

    /**
     * 评估检测结果是否命中规则条件
     * 所有条件之间为AND关系
     *
     * @param detections 检测结果列表
     * @param conditions 规则条件列表
     * @return true-命中规则, false-未命中
     */
    public static boolean evaluate(List<Detection> detections, List<RuleCondition> conditions) {
        if (conditions == null || conditions.isEmpty()) {
            return false;
        }

        // 所有条件都必须满足 (AND逻辑)
        for (RuleCondition condition : conditions) {
            if (!evaluateCondition(detections, condition)) {
                return false;
            }
        }

        return true;
    }

    /**
     * 评估单个条件
     */
    private static boolean evaluateCondition(List<Detection> detections, RuleCondition condition) {
        String type = condition.getType();
        String operator = condition.getOperator();
        String value = condition.getValue();

        return switch (type) {
            case "target" -> evaluateTarget(detections, operator, value);
            case "confidence" -> evaluateConfidence(detections, operator, value);
            case "frames" -> evaluateFrames(detections, operator, value);
            case "zone" -> evaluateZone(detections, operator, value);
            default -> {
                log.warn("未知的条件类型: {}", type);
                yield false;
            }
        };
    }

    /**
     * 目标类别匹配
     * operator: =, in
     */
    private static boolean evaluateTarget(List<Detection> detections, String operator, String value) {
        boolean hasMatch = detections.stream()
                .anyMatch(d -> {
                    if ("=".equals(operator)) {
                        return d.getLabel().equals(value);
                    } else if ("in".equals(operator)) {
                        String[] targets = value.split(",");
                        for (String target : targets) {
                            if (d.getLabel().trim().equals(target.trim())) {
                                return true;
                            }
                        }
                    }
                    return false;
                });

        return hasMatch;
    }

    /**
     * 置信度阈值
     * operator: >, >=, <
     */
    private static boolean evaluateConfidence(List<Detection> detections, String operator, String value) {
        try {
            BigDecimal threshold = new BigDecimal(value);

            return detections.stream()
                    .anyMatch(d -> {
                        if (d.getConfidence() == null) {
                            return false;
                        }
                        int comparison = d.getConfidence().compareTo(threshold);
                        return switch (operator) {
                            case ">" -> comparison > 0;
                            case ">=" -> comparison >= 0;
                            case "<" -> comparison < 0;
                            default -> false;
                        };
                    });
        } catch (NumberFormatException e) {
            log.warn("置信度阈值格式错误: {}", value);
            return false;
        }
    }

    /**
     * 连续帧数检测
     * operator: >, >=
     */
    private static boolean evaluateFrames(List<Detection> detections, String operator, String value) {
        try {
            int threshold = Integer.parseInt(value);

            // 获取第一个检测结果的摄像头ID（简化处理）
            if (detections.isEmpty()) {
                return false;
            }

            // 这里简化处理，实际应该传入cameraId
            String cameraId = "default";

            // 检查是否有目标匹配
            boolean hasTarget = !detections.isEmpty();

            if (!hasTarget) {
                return false;
            }

            // 使用第一个目标的标签
            String label = detections.get(0).getLabel();

            // 更新帧计数
            Map<String, FrameInfo> cameraCache = frameCache.computeIfAbsent(cameraId, k -> new ConcurrentHashMap<>());
            FrameInfo frameInfo = cameraCache.get(label);

            if (frameInfo == null) {
                frameInfo = new FrameInfo(1, LocalDateTime.now());
                cameraCache.put(label, frameInfo);
            } else {
                frameInfo.count++;
                frameInfo.lastTime = LocalDateTime.now();
            }

            int count = frameInfo.count;

            return switch (operator) {
                case ">" -> count > threshold;
                case ">=" -> count >= threshold;
                default -> false;
            };
        } catch (NumberFormatException e) {
            log.warn("帧数阈值格式错误: {}", value);
            return false;
        }
    }

    /**
     * 区域检测
     * operator: in
     * value: "x1,y1,x2,y2"
     */
    private static boolean evaluateZone(List<Detection> detections, String operator, String value) {
        if (!"in".equals(operator)) {
            return false;
        }

        try {
            String[] parts = value.split(",");
            if (parts.length != 4) {
                log.warn("区域坐标格式错误: {}", value);
                return false;
            }

            double zoneX1 = Double.parseDouble(parts[0].trim());
            double zoneY1 = Double.parseDouble(parts[1].trim());
            double zoneX2 = Double.parseDouble(parts[2].trim());
            double zoneY2 = Double.parseDouble(parts[3].trim());

            // 检查是否有检测框与区域相交
            for (Detection detection : detections) {
                if (detection.getBbox() == null) {
                    continue;
                }

                String[] bboxParts = detection.getBbox().split(",");
                if (bboxParts.length != 4) {
                    continue;
                }

                double bboxX1 = Double.parseDouble(bboxParts[0].trim());
                double bboxY1 = Double.parseDouble(bboxParts[1].trim());
                double bboxX2 = Double.parseDouble(bboxParts[2].trim());
                double bboxY2 = Double.parseDouble(bboxParts[3].trim());

                // 检查是否相交
                if (!(bboxX2 < zoneX1 || bboxX1 > zoneX2 || bboxY2 < zoneY1 || bboxY1 > zoneY2)) {
                    return true;
                }
            }

            return false;
        } catch (NumberFormatException e) {
            log.warn("区域坐标格式错误: {}", value);
            return false;
        }
    }

    /**
     * 清理过期的帧缓存
     */
    public static void cleanExpiredCache() {
        LocalDateTime now = LocalDateTime.now();
        for (Map.Entry<String, Map<String, FrameInfo>> cameraEntry : frameCache.entrySet()) {
            Map<String, FrameInfo> cameraCache = cameraEntry.getValue();
            cameraCache.entrySet().removeIf(entry -> {
                FrameInfo info = entry.getValue();
                return info.lastTime.isBefore(now.minusMinutes(5));
            });
        }
    }

    /**
     * 帧信息
     */
    private static class FrameInfo {
        int count;
        LocalDateTime lastTime;

        FrameInfo(int count, LocalDateTime lastTime) {
            this.count = count;
            this.lastTime = lastTime;
        }
    }
}
