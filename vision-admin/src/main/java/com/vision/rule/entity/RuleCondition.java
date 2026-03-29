package com.vision.rule.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 规则条件实体
 * 对应表: rule_condition
 */
@Data
@TableName("rule_condition")
public class RuleCondition {

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /**
     * 规则ID
     */
    private String ruleId;

    /**
     * 条件类型: target/confidence/frames/zone
     */
    private String type;

    /**
     * 操作符: =, >, >=, <, in
     */
    private String operator;

    /**
     * 条件值
     */
    private String value;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
