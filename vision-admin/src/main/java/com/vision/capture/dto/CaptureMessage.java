package com.vision.capture.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 抓图任务消息体（内部 DTO）
 *
 * 职责:
 * - 封装抓图任务的基本信息
 * - 在调度器和任务服务之间传递
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CaptureMessage {

    /**
     * 摄像头 ID
     */
    private String cameraId;

    /**
     * 摄像头名称
     */
    private String cameraName;

    /**
     * 视频流地址
     */
    private String streamUrl;

    /**
     * 抓图频率（秒）
     */
    private Integer captureFrequency;
}
