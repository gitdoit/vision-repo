package com.vision.task.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 监测任务创建/更新请求 DTO
 */
@Data
public class MonitorTaskCreateDTO {

    @NotBlank(message = "任务名称不能为空")
    private String name;

    private String description;

    private String businessLine;

    @NotBlank(message = "摄像头分组不能为空")
    private String groupId;

    @NotBlank(message = "模型不能为空")
    private String modelId;

    private String captureFrequency;

    private String scheduleStartTime;

    private String scheduleEndTime;

    private String scheduleWeekdays;

    private LocalDate effectiveStart;

    private LocalDate effectiveEnd;

    /** 触发目标类别，逗号分隔 */
    private String alertTarget;

    /** 置信度阈值 */
    private BigDecimal alertConfidence;

    /** 连续帧数阈值 */
    private Integer alertFrames;

    /** 告警级别: severe/warning/info */
    private String alertLevel;

    /** 推送方式，逗号分隔 */
    private String pushMethods;

    /** HTTP 回调地址 */
    private String callbackUrl;

    /** 自定义请求头 JSON 字符串 */
    private String callbackHeaders;

    /** 指定节点ID列表，逗号分隔 */
    private String nodeIds;
}
