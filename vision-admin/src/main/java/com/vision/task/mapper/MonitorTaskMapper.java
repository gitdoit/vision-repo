package com.vision.task.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vision.task.entity.MonitorTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 监测任务 Mapper
 */
@Mapper
public interface MonitorTaskMapper extends BaseMapper<MonitorTask> {

    /**
     * 原子递增推理计数
     */
    @Update("UPDATE monitor_task SET total_inference = total_inference + 1, last_inference_time = #{time} WHERE id = #{taskId}")
    void incrementInference(@Param("taskId") String taskId, @Param("time") java.time.LocalDateTime time);

    /**
     * 原子递增告警计数
     */
    @Update("UPDATE monitor_task SET total_alert = total_alert + 1, last_alert_time = #{time} WHERE id = #{taskId}")
    void incrementAlert(@Param("taskId") String taskId, @Param("time") java.time.LocalDateTime time);
}
