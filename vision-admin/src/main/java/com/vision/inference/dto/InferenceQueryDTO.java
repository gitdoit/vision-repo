package com.vision.inference.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 推理记录查询DTO
 */
@Data
public class InferenceQueryDTO {

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    /**
     * 业务线
     */
    private String businessLine;

    /**
     * 摄像头ID
     */
    private String cameraId;

    /**
     * 告警类型
     */
    private String alertType;

    /**
     * 监测任务ID
     */
    private String taskId;

    /**
     * 最小置信度
     */
    private Double minConfidence;

    /**
     * 最大置信度
     */
    private Double maxConfidence;

    /**
     * 页码
     */
    private Integer page = 1;

    /**
     * 每页大小
     */
    private Integer size = 20;
}
