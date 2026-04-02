package com.vision.task.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 监测任务响应 VO
 */
@Data
public class MonitorTaskVO {

    private String id;

    private String name;

    private String description;

    private String businessLine;

    private String groupId;

    /** 分组名称（冗余展示） */
    private String groupName;

    private String modelId;

    /** 模型名称（冗余展示） */
    private String modelName;

    private String status;

    private String captureFrequency;

    private String scheduleStartTime;

    private String scheduleEndTime;

    private String scheduleWeekdays;

    private LocalDate effectiveStart;

    private LocalDate effectiveEnd;

    private String alertTarget;

    private BigDecimal alertConfidence;

    private Integer alertFrames;

    private String alertLevel;

    private String pushMethods;

    private String callbackUrl;

    private String callbackHeaders;

    private String nodeIds;

    /** 模型分类名称列表 */
    private List<String> modelClassNames;

    /** 模型输入分辨率 */
    private String modelInputResolution;

    /** 模型任务类型 (detect/segment/classify/pose) */
    private String modelTaskType;

    /** 关联推理节点简要信息 */
    private List<TaskNodeInfo> nodes;

    /** 累计分析次数 */
    private Long totalInference;

    /** 累计告警次数 */
    private Long totalAlert;

    /** 最后一次分析时间 */
    private LocalDateTime lastInferenceTime;

    /** 最后一次告警时间 */
    private LocalDateTime lastAlertTime;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
