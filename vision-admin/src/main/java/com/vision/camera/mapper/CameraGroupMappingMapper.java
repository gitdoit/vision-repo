package com.vision.camera.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vision.camera.entity.CameraGroupMapping;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 摄像头-分组关联 Mapper
 */
@Mapper
public interface CameraGroupMappingMapper extends BaseMapper<CameraGroupMapping> {

    @Select("SELECT camera_id FROM camera_group_mapping WHERE group_id IN (#{groupIds})")
    List<String> selectCameraIdsByGroupIds(@Param("groupIds") List<String> groupIds);
}
