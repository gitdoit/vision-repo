package com.vision.camera.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vision.camera.entity.Camera;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 摄像头 Mapper
 */
@Mapper
public interface CameraMapper extends BaseMapper<Camera> {

    /**
     * 查询属于指定分组ID列表的摄像头ID（定义在 XML 中）
     */
    List<String> selectCameraIdsByGroupIds(@Param("groupIds") List<String> groupIds);

    /**
     * 根据状态查询摄像头列表
     */
    @Select("SELECT * FROM camera WHERE status = #{status} ORDER BY created_at DESC")
    List<Camera> selectByStatus(@Param("status") String status);

    /**
     * 查询所有活跃摄像头（AI启用且在线）
     */
    @Select("SELECT * FROM camera WHERE ai_enabled = true AND status = 'online' ORDER BY created_at DESC")
    List<Camera> selectActiveCameras();

    /**
     * 更新最后抓图时间
     */
    @Update("UPDATE camera SET last_capture_time = #{lastCaptureTime} WHERE id = #{cameraId}")
    void updateLastCaptureTime(@Param("cameraId") String cameraId, @Param("lastCaptureTime") java.time.LocalDateTime lastCaptureTime);
}
