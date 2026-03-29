package com.vision.dashboard.dto;

import lombok.Data;

/**
 * 仪表板统计概览VO
 */
@Data
public class DashboardStatsVO {

    /**
     * 今日分析次数
     */
    private Long todayInferenceCount;

    /**
     * 今日分析次数环比变化
     */
    private Double todayInferenceChange;

    /**
     * 今日告警数量
     */
    private Long todayAlertCount;

    /**
     * 今日告警数量环比变化
     */
    private Double todayAlertChange;

    /**
     * 摄像头总数
     */
    private Long totalCameraCount;

    /**
     * 启用AI的摄像头数
     */
    private Long aiEnabledCameraCount;
}
