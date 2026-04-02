package com.vision.dashboard.controller;

import com.vision.alert.dto.AlertVO;
import com.vision.dashboard.dto.AlertRankingVO;
import com.vision.dashboard.dto.DashboardStatsVO;
import com.vision.dashboard.dto.SystemHealthVO;
import com.vision.dashboard.dto.WeeklyTrendVO;
import com.vision.dashboard.service.DashboardService;
import com.vision.common.response.R;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 仪表板控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * 获取统计概览
     */
    @GetMapping("/stats")
    public R<DashboardStatsVO> getStats() {
        log.info("获取统计概览");
        DashboardStatsVO stats = dashboardService.getStats();
        return R.ok(stats);
    }

    /**
     * 获取周趋势
     */
    @GetMapping("/weekly-trend")
    public R<List<WeeklyTrendVO>> getWeeklyTrend() {
        List<WeeklyTrendVO> trend = dashboardService.getWeeklyTrend();
        return R.ok(trend);
    }

    /**
     * 获取告警排名
     */
    @GetMapping("/alert-ranking")
    public R<List<AlertRankingVO>> getAlertRanking() {
        List<AlertRankingVO> ranking = dashboardService.getAlertRanking();
        return R.ok(ranking);
    }

    /**
     * 获取实时告警列表
     */
    @GetMapping("/realtime-alerts")
    public R<List<AlertVO>> getRealtimeAlerts(
            @RequestParam(defaultValue = "10") Integer limit) {

        List<AlertVO> alerts = dashboardService.getRealtimeAlerts(limit);
        return R.ok(alerts);
    }

    /**
     * 获取系统健康概览
     */
    @GetMapping("/system-health")
    public R<SystemHealthVO> getSystemHealth() {
        SystemHealthVO health = dashboardService.getSystemHealth();
        return R.ok(health);
    }
}
