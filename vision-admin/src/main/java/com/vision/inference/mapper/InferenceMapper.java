package com.vision.inference.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vision.inference.entity.InferenceRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 推理记录 Mapper
 */
@Mapper
public interface InferenceMapper extends BaseMapper<InferenceRecord> {

    /**
     * 分页查询推理记录
     */
    List<InferenceRecord> selectPageByCondition(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("cameraId") String cameraId,
            @Param("alertStatus") String alertStatus,
            @Param("taskId") String taskId,
            @Param("size") Integer size,
            @Param("offset") Integer offset);

    /**
     * 统计符合条件的记录总数
     */
    Long countByCondition(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("cameraId") String cameraId,
            @Param("alertStatus") String alertStatus,
            @Param("taskId") String taskId);

    /**
     * 统计时间范围内的推理次数
     */
    Long countByTimeRange(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
}
