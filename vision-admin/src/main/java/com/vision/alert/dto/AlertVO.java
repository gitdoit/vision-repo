package com.vision.alert.dto;

import com.vision.alert.entity.Alert;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 告警视图对象
 */
@Data
public class AlertVO {

    private String id;
    private String alertLevel;
    private String alertType;
    private String scene;
    private String cameraId;
    private String cameraName;
    private String streamId;
    private LocalDateTime captureTime;
    private LocalDateTime alertTime;
    private String triggerCondition;
    private String relatedObjects;
    private String evidence;
    private String location;
    private String ruleId;
    private Boolean readStatus;
    private LocalDateTime createdAt;

    /**
     * 从实体转换
     */
    public static AlertVO fromEntity(Alert entity) {
        AlertVO vo = new AlertVO();
        vo.setId(entity.getId());
        vo.setAlertLevel(entity.getAlertLevel());
        vo.setAlertType(entity.getAlertType());
        vo.setScene(entity.getScene());
        vo.setCameraId(entity.getCameraId());
        vo.setStreamId(entity.getStreamId());
        vo.setCaptureTime(entity.getCaptureTime());
        vo.setAlertTime(entity.getAlertTime());
        vo.setTriggerCondition(entity.getTriggerCondition());
        vo.setRelatedObjects(entity.getRelatedObjects());
        vo.setEvidence(entity.getEvidence());
        vo.setLocation(entity.getLocation());
        vo.setRuleId(entity.getRuleId());
        vo.setReadStatus(entity.getReadStatus());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
