package com.vision.rule.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 规则实体
 * 对应表: rule
 */
@Data
@TableName("rule")
public class Rule {

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /**
     * 规则名称
     */
    private String name;

    /**
     * 业务线
     */
    private String businessLine;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 优先级: severe/warning/info
     */
    private String priority;

    /**
     * 生效时间段: '10:00-18:00' 或 '全时段'
     */
    private String schedule;

    /**
     * 生效星期: '1,2,3,4,5'
     */
    private String weekdays;

    /**
     * 生效开始日期
     */
    private LocalDate effectiveStart;

    /**
     * 生效结束日期
     */
    private LocalDate effectiveEnd;

    /**
     * 动作配置 (JSONB)
     */
    private String actions;

    /**
     * 是否已部署
     */
    private Boolean deployed;

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
