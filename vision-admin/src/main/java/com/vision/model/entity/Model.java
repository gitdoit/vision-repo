package com.vision.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 模型实体
 * 对应表: model
 */
@Data
@TableName("model")
public class Model {

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /**
     * 模型名称
     */
    private String name;

    /**
     * 版本号
     */
    private String version;

    /**
     * 业务标签
     */
    private String businessTag;

    /**
     * 模型任务类型: detect/segment/classify/pose
     */
    private String taskType;

    /**
     * 支持的推理引擎: TRT,ONNX
     */
    private String engineSupport;

    /**
     * 目标硬件平台
     */
    private String targetHardware;

    /**
     * 类别名称，逗号分隔: "person,car,bicycle"
     */
    private String classNames;

    /**
     * 类别总数
     */
    private Integer numClasses;

    /**
     * 模型解析状态: pending / parsed / failed
     */
    private String parsedStatus;

    /**
     * 置信度阈值
     */
    private BigDecimal confidenceThreshold;

    /**
     * 输入分辨率: 640x640
     */
    private String inputResolution;

    /**
     * 最大并发数
     */
    private Integer maxConcurrency;

    /**
     * 模型文件路径
     */
    private String modelPath;

    /**
     * 作者
     */
    private String author;

    /**
     * 平均延迟(ms)
     */
    private Integer avgLatency;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
