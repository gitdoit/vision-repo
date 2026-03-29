package com.vision.inference.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vision.inference.entity.Detection;
import org.apache.ibatis.annotations.Mapper;

/**
 * 检测目标 Mapper
 */
@Mapper
public interface DetectionMapper extends BaseMapper<Detection> {
}
