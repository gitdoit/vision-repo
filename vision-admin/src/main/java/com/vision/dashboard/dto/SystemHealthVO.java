package com.vision.dashboard.dto;

import lombok.Data;

/**
 * 系统健康概览VO
 */
@Data
public class SystemHealthVO {

    private long onlineNodeCount;
    private long totalNodeCount;
    private long deployedModelCount;
    private long totalModelCount;
    private long runningTaskCount;
    private long stoppedTaskCount;
    private long errorTaskCount;
    private long unreadAlertCount;
}
