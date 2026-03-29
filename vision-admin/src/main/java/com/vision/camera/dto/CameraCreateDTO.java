package com.vision.camera.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 摄像头创建/更新DTO
 */
@Data
public class CameraCreateDTO {

    /**
     * 摄像头名称
     */
    @NotBlank(message = "摄像头名称不能为空")
    private String name;

    /**
     * 业务线
     */
    @NotBlank(message = "业务线不能为空")
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
     * 分组ID
     */
    private String groupId;

    /**
     * 视频平台ID
     */
    private String platformId;

    /**
     * 通道号
     */
    private String channelNo;
}
