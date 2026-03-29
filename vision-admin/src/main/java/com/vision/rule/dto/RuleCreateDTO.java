package com.vision.rule.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 规则创建/更新DTO
 */
@Data
public class RuleCreateDTO {

    /**
     * 规则名称
     */
    @NotBlank(message = "规则名称不能为空")
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
     * 动作配置 (JSON字符串)
     */
    private String actions;

    /**
     * 规则条件列表
     */
    @NotEmpty(message = "规则条件不能为空")
    @Valid
    private List<RuleConditionDTO> conditions;
}
