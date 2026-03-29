package com.vision.rule.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 规则条件DTO
 */
@Data
public class RuleConditionDTO {

    /**
     * 条件类型: target/confidence/frames/zone
     */
    @NotBlank(message = "条件类型不能为空")
    private String type;

    /**
     * 操作符: =, >, >=, <, in
     */
    @NotBlank(message = "操作符不能为空")
    private String operator;

    /**
     * 条件值
     */
    @NotBlank(message = "条件值不能为空")
    private String value;
}
