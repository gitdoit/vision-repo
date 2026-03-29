package com.vision.rule.dto;

import com.vision.rule.entity.Rule;
import com.vision.rule.entity.RuleCondition;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 规则视图对象
 */
@Data
public class RuleVO {

    private String id;
    private String name;
    private String businessLine;
    private Boolean enabled;
    private String priority;
    private String schedule;
    private String weekdays;
    private LocalDate effectiveStart;
    private LocalDate effectiveEnd;
    private String actions;
    private Boolean deployed;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 规则条件列表
     */
    private List<RuleConditionDTO> conditions = new ArrayList<>();

    /**
     * 从实体转换
     */
    public static RuleVO fromEntity(Rule entity) {
        RuleVO vo = new RuleVO();
        vo.setId(entity.getId());
        vo.setName(entity.getName());
        vo.setBusinessLine(entity.getBusinessLine());
        vo.setEnabled(entity.getEnabled());
        vo.setPriority(entity.getPriority());
        vo.setSchedule(entity.getSchedule());
        vo.setWeekdays(entity.getWeekdays());
        vo.setEffectiveStart(entity.getEffectiveStart());
        vo.setEffectiveEnd(entity.getEffectiveEnd());
        vo.setActions(entity.getActions());
        vo.setDeployed(entity.getDeployed());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    /**
     * 从条件实体转换
     */
    public static RuleConditionDTO fromConditionEntity(RuleCondition entity) {
        RuleConditionDTO dto = new RuleConditionDTO();
        dto.setType(entity.getType());
        dto.setOperator(entity.getOperator());
        dto.setValue(entity.getValue());
        return dto;
    }
}
