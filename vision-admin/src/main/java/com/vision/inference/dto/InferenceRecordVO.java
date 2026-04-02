package com.vision.inference.dto;

import com.vision.inference.entity.InferenceRecord;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 推理记录视图对象
 */
@Data
public class InferenceRecordVO {

    private String id;
    private String eventId;
    private String cameraId;
    private String cameraName;
    private String businessType;
    private BigDecimal avgConfidence;
    private String alertStatus;
    private String thumbnailUrl;
    private String originalImageUrl;
    private String annotatedImageUrl;
    private String rawJson;
    private String modelName;
    private Integer inferenceTimeMs;
    private Integer captureTimeMs;
    private String taskId;
    private String taskName;
    private String groupName;
    private LocalDateTime createdAt;

    /**
     * 检测目标列表
     */
    private List<DetectionVO> detections = new ArrayList<>();

    /**
     * 从实体转换
     */
    public static InferenceRecordVO fromEntity(InferenceRecord entity) {
        InferenceRecordVO vo = new InferenceRecordVO();
        vo.setId(entity.getId());
        vo.setEventId(entity.getEventId());
        vo.setCameraId(entity.getCameraId());
        vo.setBusinessType(entity.getBusinessType());
        vo.setAvgConfidence(entity.getAvgConfidence());
        vo.setAlertStatus(entity.getAlertStatus());
        vo.setThumbnailUrl(entity.getThumbnailUrl());
        vo.setOriginalImageUrl(entity.getOriginalImageUrl());
        vo.setAnnotatedImageUrl(entity.getAnnotatedImageUrl());
        vo.setRawJson(entity.getRawJson());
        vo.setModelName(entity.getModelName());
        vo.setInferenceTimeMs(entity.getInferenceTimeMs());
        vo.setCaptureTimeMs(entity.getCaptureTimeMs());
        vo.setTaskId(entity.getTaskId());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
