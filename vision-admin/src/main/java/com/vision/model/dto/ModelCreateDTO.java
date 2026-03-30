package com.vision.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 模型创建/更新DTO
 */
@Data
public class ModelCreateDTO {

    /**
     * 模型名称
     */
    @NotBlank(message = "模型名称不能为空")
    private String name;

    /**
     * 版本号
     */
    @NotBlank(message = "版本号不能为空")
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
     * 支持的推理引擎
     */
    private String engineSupport;

    /**
     * 目标硬件平台
     */
    private String targetHardware;

    /**
     * 模型文件路径
     */
    private String modelPath;

    /**
     * 作者
     */
    private String author;
}
