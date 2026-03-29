package com.vision.inference.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vision.inference.entity.InferenceRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

/**
 * 推理记录 Mapper
 */
@Mapper
public interface InferenceMapper extends BaseMapper<InferenceRecord> {

    /**
     * 分页查询推理记录
     */
    @Select("<script>" +
            "SELECT * FROM inference_record " +
            "WHERE 1=1 " +
            "<if test='startTime != null'>AND created_at &gt;= #{startTime}</if> " +
            "<if test='endTime != null'>AND created_at &lt;= #{endTime}</if> " +
            "<if test='cameraId != null'>AND camera_id = #{cameraId}</if> " +
            "<if test='alertStatus != null'>AND alert_status = #{alertStatus}</if> " +
            "ORDER BY created_at DESC " +
            "LIMIT #{size} OFFSET #{offset}" +
            "</script>")
    IPage<InferenceRecord> selectPageByCondition(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("cameraId") String cameraId,
            @Param("alertStatus") String alertStatus,
            @Param("size") Integer size,
            @Param("offset") Integer offset);

    /**
     * 统计时间范围内的推理次数
     */
    @Select("SELECT COUNT(*) FROM inference_record WHERE created_at >= #{startTime} AND created_at <= #{endTime}")
    Integer countByTimeRange(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
}
