package com.vision.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 模型版本历史实体
 * 对应表: model_version
 */
@Data
@TableName("model_version")
public class ModelVersion {

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /**
     * 模型ID
     */
    private String modelId;

    /**
     * 版本号
     */
    private String version;

    /**
     * 版本描述
     */
    private String description;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
