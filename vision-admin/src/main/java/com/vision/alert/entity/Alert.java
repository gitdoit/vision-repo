package com.vision.alert.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 告警实体
 * 对应表: alert
 */
@Data
@TableName("alert")
public class Alert {

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /**
     * 监测任务ID
     */
    private String taskId;

    /**
     * 告警级别: critical/warning/info
     */
    private String alertLevel;

    /**
     * 告警类型
     */
    private String alertType;

    /**
     * 场景
     */
    private String scene;

    /**
     * 摄像头ID
     */
    private String cameraId;

    /**
     * 流ID
     */
    private String streamId;

    /**
     * 抓拍时间
     */
    private LocalDateTime captureTime;

    /**
     * 告警时间
     */
    private LocalDateTime alertTime;

    /**
     * 触发条件描述
     */
    private String triggerCondition;

    /**
     * 关联目标 (JSONB)
     */
    private String relatedObjects;

    /**
     * 证据 (JSONB)
     */
    private String evidence;

    /**
     * 位置信息 (JSONB)
     */
    private String location;

    /**
     * 规则ID
     */
    private String ruleId;

    /**
     * 是否已读
     */
    private Boolean readStatus;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
