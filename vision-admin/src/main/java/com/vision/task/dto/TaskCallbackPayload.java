package com.vision.task.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 监测任务告警回调报文 — 固定结构
 * 推送给业务平台的 HTTP 回调内容
 */
@Data
public class TaskCallbackPayload {

    private String alertId;

    private String alertLevel;

    private String alertType;

    private LocalDateTime alertTime;

    private LocalDateTime captureTime;

    private String triggerCondition;

    private TaskInfo task;

    private CameraInfo camera;

    private DetectionInfo detection;

    private EvidenceInfo evidence;

    @Data
    public static class TaskInfo {
        private String taskId;
        private String taskName;
        private String businessLine;
    }

    @Data
    public static class CameraInfo {
        private String cameraId;
        private String cameraName;
        private String streamId;
        private String location;
    }

    @Data
    public static class DetectionInfo {
        private String modelName;
        private Integer inferenceTimeMs;
        private List<TargetInfo> targets;
    }

    @Data
    public static class TargetInfo {
        private String className;
        private Double confidence;
        private String bbox;
    }

    @Data
    public static class EvidenceInfo {
        private String imageUrl;
        private String annotatedImageUrl;
    }
}
