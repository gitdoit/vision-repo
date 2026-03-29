package com.vision.rule.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vision.rule.dto.RuleConditionDTO;
import com.vision.rule.dto.RuleCreateDTO;
import com.vision.rule.dto.RuleVO;
import com.vision.rule.entity.Rule;
import com.vision.rule.entity.RuleCondition;
import com.vision.rule.mapper.RuleConditionMapper;
import com.vision.rule.mapper.RuleMapper;
import com.vision.common.exception.BizException;
import com.vision.common.util.IdUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 规则服务
 */
@Service
@RequiredArgsConstructor
public class RuleService extends ServiceImpl<RuleMapper, Rule> {

    private final RuleMapper ruleMapper;
    private final RuleConditionMapper ruleConditionMapper;

    /**
     * 分页查询规则列表
     */
    public IPage<RuleVO> pageRules(Integer page, Integer size, String businessLine, Boolean enabled) {
        Page<Rule> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Rule> wrapper = new LambdaQueryWrapper<>();

        if (businessLine != null && !businessLine.isEmpty()) {
            wrapper.eq(Rule::getBusinessLine, businessLine);
        }
        if (enabled != null) {
            wrapper.eq(Rule::getEnabled, enabled);
        }

        wrapper.orderByDesc(Rule::getCreatedAt);

        IPage<Rule> rulePage = ruleMapper.selectPage(pageParam, wrapper);

        List<RuleVO> voList = rulePage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        Page<RuleVO> resultPage = new Page<>(rulePage.getCurrent(), rulePage.getSize(), rulePage.getTotal());
        resultPage.setRecords(voList);
        return resultPage;
    }

    /**
     * 根据ID查询规则详情
     */
    public RuleVO getRuleById(String id) {
        Rule rule = ruleMapper.selectById(id);
        if (rule == null) {
            throw new BizException("规则不存在");
        }
        return convertToVO(rule);
    }

    /**
     * 创建规则
     */
    @Transactional(rollbackFor = Exception.class)
    public RuleVO createRule(RuleCreateDTO dto) {
        Rule rule = new Rule();
        BeanUtils.copyProperties(dto, rule);
        rule.setId(IdUtil.uuid());
        rule.setEnabled(dto.getEnabled() != null ? dto.getEnabled() : true);
        rule.setPriority(dto.getPriority() != null ? dto.getPriority() : "warning");
        rule.setSchedule(dto.getSchedule() != null ? dto.getSchedule() : "全时段");
        rule.setDeployed(false);

        ruleMapper.insert(rule);

        // 保存规则条件
        saveRuleConditions(rule.getId(), dto.getConditions());

        return convertToVO(rule);
    }

    /**
     * 更新规则
     */
    @Transactional(rollbackFor = Exception.class)
    public RuleVO updateRule(String id, RuleCreateDTO dto) {
        Rule rule = ruleMapper.selectById(id);
        if (rule == null) {
            throw new BizException("规则不存在");
        }

        BeanUtils.copyProperties(dto, rule);
        rule.setDeployed(false); // 更新后需要重新部署
        ruleMapper.updateById(rule);

        // 删除旧条件，保存新条件
        LambdaQueryWrapper<RuleCondition> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RuleCondition::getRuleId, id);
        ruleConditionMapper.delete(wrapper);

        saveRuleConditions(id, dto.getConditions());

        return convertToVO(rule);
    }

    /**
     * 删除规则
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteRule(String id) {
        Rule rule = ruleMapper.selectById(id);
        if (rule == null) {
            throw new BizException("规则不存在");
        }

        // 删除规则条件
        LambdaQueryWrapper<RuleCondition> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RuleCondition::getRuleId, id);
        ruleConditionMapper.delete(wrapper);

        ruleMapper.deleteById(id);
    }

    /**
     * 部署规则
     */
    @Transactional(rollbackFor = Exception.class)
    public void deployRule(String id) {
        Rule rule = ruleMapper.selectById(id);
        if (rule == null) {
            throw new BizException("规则不存在");
        }

        rule.setDeployed(true);
        ruleMapper.updateById(rule);
    }

    /**
     * 测试规则
     */
    public boolean testRule(String id, List<com.vision.inference.entity.Detection> detections) {
        Rule rule = ruleMapper.selectById(id);
        if (rule == null) {
            throw new BizException("规则不存在");
        }

        // 获取规则条件
        LambdaQueryWrapper<RuleCondition> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RuleCondition::getRuleId, id);
        List<RuleCondition> conditions = ruleConditionMapper.selectList(wrapper);

        // 使用RuleEvaluator评估
        return RuleEvaluator.evaluate(detections, conditions);
    }

    /**
     * 获取生效的规则列表
     */
    public List<Rule> getActiveRules() {
        LambdaQueryWrapper<Rule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Rule::getEnabled, true);
        wrapper.eq(Rule::getDeployed, true);

        LocalDate now = LocalDate.now();
        wrapper.and(w -> w.isNull(Rule::getEffectiveStart)
                .or()
                .le(Rule::getEffectiveStart, now));

        wrapper.and(w -> w.isNull(Rule::getEffectiveEnd)
                .or()
                .ge(Rule::getEffectiveEnd, now));

        return ruleMapper.selectList(wrapper);
    }

    /**
     * 保存规则条件
     */
    private void saveRuleConditions(String ruleId, List<RuleConditionDTO> conditionDTOs) {
        for (RuleConditionDTO dto : conditionDTOs) {
            RuleCondition condition = new RuleCondition();
            condition.setId(IdUtil.uuid());
            condition.setRuleId(ruleId);
            condition.setType(dto.getType());
            condition.setOperator(dto.getOperator());
            condition.setValue(dto.getValue());
            condition.setCreatedAt(LocalDateTime.now());
            ruleConditionMapper.insert(condition);
        }
    }

    /**
     * 转换为VO
     */
    private RuleVO convertToVO(Rule rule) {
        RuleVO vo = RuleVO.fromEntity(rule);

        // 加载规则条件
        LambdaQueryWrapper<RuleCondition> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RuleCondition::getRuleId, rule.getId());
        List<RuleCondition> conditions = ruleConditionMapper.selectList(wrapper);

        List<RuleConditionDTO> conditionDTOs = conditions.stream()
                .map(RuleVO::fromConditionEntity)
                .collect(Collectors.toList());

        vo.setConditions(conditionDTOs);

        return vo;
    }
}
