package com.vision.rule.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vision.rule.entity.Rule;
import org.apache.ibatis.annotations.Mapper;

/**
 * 规则 Mapper
 */
@Mapper
public interface RuleMapper extends BaseMapper<Rule> {
}
