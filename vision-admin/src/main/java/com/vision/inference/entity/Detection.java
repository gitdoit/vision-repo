package com.vision.inference.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 检测目标实体
 * 对应表: detection
 */
@Data
@TableName("detection")
public class Detection {

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /**
     * 推理记录ID
     */
    private String recordId;

    /**
     * 目标标签
     */
    private String label;

    /**
     * 置信度
     */
    private BigDecimal confidence;

    /**
     * 边界框: 'x1,y1,x2,y2'
     */
    private String bbox;

    /**
     * 数量
     */
    private Integer count;

    /**
     * 额外属性 (JSONB)
     */
    private String attributes;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
