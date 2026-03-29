package com.vision.alert.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 告警查询DTO
 */
@Data
public class AlertQueryDTO {

    /**
     * 告警级别
     */
    private String alertLevel;

    /**
     * 告警类型
     */
    private String alertType;

    /**
     * 摄像头ID
     */
    private String cameraId;

    /**
     * 是否已读
     */
    private Boolean readStatus;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    /**
     * 页码
     */
    private Integer page = 1;

    /**
     * 每页大小
     */
    private Integer size = 20;
}
