package com.vision.task.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.vision.common.handler.JsonbTypeHandler;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 监测任务实体
 * 对应表: monitor_task
 */
@Data
@TableName(value = "monitor_task", autoResultMap = true)
public class MonitorTask {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /** 任务名称 */
    private String name;

    /** 任务描述 */
    private String description;

    /** 业务线标签 */
    private String businessLine;

    /** 关联摄像头分组ID */
    private String groupId;

    /** 关联模型ID */
    private String modelId;

    /** 状态: stopped/running/error */
    private String status;

    /** 抓图频率（覆盖摄像头配置） */
    private String captureFrequency;

    /** 生效时段起始 如 "08:00" */
    private String scheduleStartTime;

    /** 生效时段结束 如 "22:00" */
    private String scheduleEndTime;

    /** 生效星期 如 "1,2,3,4,5" */
    private String scheduleWeekdays;

    /** 生效日期起 */
    private LocalDate effectiveStart;

    /** 生效日期止 */
    private LocalDate effectiveEnd;

    /** 触发目标类别，逗号分隔 */
    private String alertTarget;

    /** 置信度阈值 */
    private BigDecimal alertConfidence;

    /** 连续帧数阈值 */
    private Integer alertFrames;

    /** 告警级别: severe/warning/info */
    private String alertLevel;

    /** 推送方式，逗号分隔 如 "http_callback,websocket" */
    private String pushMethods;

    /** HTTP 回调地址 */
    private String callbackUrl;

    /** 自定义请求头 (JSONB) */
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String callbackHeaders;

    /** 指定节点ID列表，逗号分隔，空=自动调度 */
    private String nodeIds;

    /** 累计分析次数 */
    private Long totalInference;

    /** 累计告警次数 */
    private Long totalAlert;

    /** 最后一次分析时间 */
    private LocalDateTime lastInferenceTime;

    /** 最后一次告警时间 */
    private LocalDateTime lastAlertTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
