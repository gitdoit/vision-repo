package com.vision.task.service;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 预截帧结果
 *
 * 由 CaptureWorker 产生，存入 CaptureCoordinator 帧缓冲，
 * 供 TaskScheduler 消费给各任务的推理管道。
 */
@Data
public class CaptureFrame {

    /** 帧唯一标识（UUID），用于防止同一物理帧被同一任务重复消费 */
    private String frameId;

    /** 摄像头 ID */
    private String cameraId;

    /** 上传到 StorageService 后的访问 URL */
    private String imageUrl;

    /** 截帧时间 */
    private LocalDateTime captureTime;

    /** 截帧耗时（毫秒） */
    private int captureTimeMs;

    /** 截帧状态: success / failed */
    private String status;

    public boolean isSuccess() {
        return "success".equals(status);
    }
}
