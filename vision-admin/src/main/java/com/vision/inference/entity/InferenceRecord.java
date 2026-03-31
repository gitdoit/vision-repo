package com.vision.inference.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 推理记录实体
 * 对应表: inference_record
 */
@Data
@TableName("inference_record")
public class InferenceRecord {

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
     * 事件ID
     */
    private String eventId;

    /**
     * 摄像头ID
     */
    private String cameraId;

    /**
     * 业务类型
     */
    private String businessType;

    /**
     * 平均置信度
     */
    private BigDecimal avgConfidence;

    /**
     * 告警状态: normal/warning/alert
     */
    private String alertStatus;

    /**
     * 缩略图URL
     */
    private String thumbnailUrl;

    /**
     * 原始图片URL
     */
    private String originalImageUrl;

    /**
     * 标注图片URL
     */
    private String annotatedImageUrl;

    /**
     * 完整推理原始结果 (JSONB)
     */
    private String rawJson;

    /**
     * 模型名称
     */
    private String modelName;

    /**
     * 推理耗时(ms)
     */
    private Integer inferenceTimeMs;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
