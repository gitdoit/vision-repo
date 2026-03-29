package com.vision.alert.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vision.alert.entity.Alert;
import org.apache.ibatis.annotations.Mapper;

/**
 * 告警 Mapper
 */
@Mapper
public interface AlertMapper extends BaseMapper<Alert> {
}
