package com.vision.camera.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 摄像头实体
 * 对应表: camera
 */
@Data
@TableName("camera")
public class Camera {

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /**
     * 摄像头名称
     */
    private String name;

    /**
     * 业务线
     */
    private String businessLine;

    /**
     * 安装位置
     */
    private String location;

    /**
     * 视频流地址 (RTSP)
     */
    private String streamUrl;

    /**
     * 抓图频率: 1s, 5s, 1min, 5min
     */
    private String captureFrequency;

    /**
     * 是否启用AI分析
     */
    private Boolean aiEnabled;

    /**
     * 状态: online/offline/error
     */
    private String status;

    /**
     * 最后抓图时间
     */
    private LocalDateTime lastCaptureTime;

    /**
     * 分组ID
     */
    private String groupId;

    /**
     * 来源: manual-手动添加, synced-平台同步
     */
    private String source;

    /**
     * 视频平台ID
     */
    private String platformId;

    /**
     * 通道号
     */
    private String channelNo;

    /**
     * 标签名称（来自视频平台）
     */
    private String label;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
