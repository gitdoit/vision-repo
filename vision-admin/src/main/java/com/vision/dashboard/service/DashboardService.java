package com.vision.dashboard.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.vision.alert.dto.AlertVO;
import com.vision.alert.entity.Alert;
import com.vision.alert.mapper.AlertMapper;
import com.vision.alert.service.AlertService;
import com.vision.camera.entity.Camera;
import com.vision.camera.mapper.CameraMapper;
import com.vision.dashboard.dto.AlertRankingVO;
import com.vision.dashboard.dto.DashboardStatsVO;
import com.vision.dashboard.dto.SystemHealthVO;
import com.vision.dashboard.dto.WeeklyTrendVO;
import com.vision.inference.entity.InferenceRecord;
import com.vision.inference.mapper.InferenceMapper;
import com.vision.model.entity.ModelNodeDeployment;
import com.vision.model.mapper.ModelMapper;
import com.vision.model.mapper.ModelNodeDeploymentMapper;
import com.vision.node.entity.InferenceNode;
import com.vision.node.mapper.InferenceNodeMapper;
import com.vision.task.entity.MonitorTask;
import com.vision.task.mapper.MonitorTaskMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 仪表板服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final InferenceMapper inferenceMapper;
    private final AlertMapper alertMapper;
    private final CameraMapper cameraMapper;
    private final AlertService alertService;
    private final InferenceNodeMapper inferenceNodeMapper;
    private final ModelMapper modelMapper;
    private final ModelNodeDeploymentMapper modelNodeDeploymentMapper;
    private final MonitorTaskMapper monitorTaskMapper;

    /**
     * 获取统计概览
     */
    public DashboardStatsVO getStats() {
        log.debug("开始获取统计概览数据");
        DashboardStatsVO stats = new DashboardStatsVO();

        // 今日时间范围
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime todayEnd = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);
        LocalDateTime yesterdayStart = todayStart.minusDays(1);
        LocalDateTime yesterdayEnd = todayEnd.minusDays(1);

        // 今日分析次数
        Long todayCount = inferenceMapper.countByTimeRange(todayStart, todayEnd);
        stats.setTodayInferenceCount(todayCount);

        // 昨日分析次数
        Long yesterdayCount = inferenceMapper.countByTimeRange(yesterdayStart, yesterdayEnd);
        if (yesterdayCount > 0) {
            stats.setTodayInferenceChange((double) (todayCount - yesterdayCount) / yesterdayCount * 100);
        } else {
            stats.setTodayInferenceChange(0.0);
        }

        // 今日告警数量
        LambdaQueryWrapper<Alert> alertWrapper = new LambdaQueryWrapper<>();
        alertWrapper.ge(Alert::getAlertTime, todayStart);
        alertWrapper.le(Alert::getAlertTime, todayEnd);
        Long todayAlertCount = alertMapper.selectCount(alertWrapper);
        stats.setTodayAlertCount(todayAlertCount);

        // 昨日告警数量
        LambdaQueryWrapper<Alert> yesterdayAlertWrapper = new LambdaQueryWrapper<>();
        yesterdayAlertWrapper.ge(Alert::getAlertTime, yesterdayStart);
        yesterdayAlertWrapper.le(Alert::getAlertTime, yesterdayEnd);
        Long yesterdayAlertCount = alertMapper.selectCount(yesterdayAlertWrapper);
        if (yesterdayAlertCount > 0) {
            stats.setTodayAlertChange((double) (todayAlertCount - yesterdayAlertCount) / yesterdayAlertCount * 100);
        } else {
            stats.setTodayAlertChange(0.0);
        }

        // 摄像头总数
        LambdaQueryWrapper<Camera> cameraWrapper = new LambdaQueryWrapper<>();
        stats.setTotalCameraCount(cameraMapper.selectCount(cameraWrapper));

        // 启用AI的摄像头数
        LambdaQueryWrapper<Camera> aiCameraWrapper = new LambdaQueryWrapper<>();
        aiCameraWrapper.eq(Camera::getAiEnabled, true);
        stats.setAiEnabledCameraCount(cameraMapper.selectCount(aiCameraWrapper));

        return stats;
    }

    /**
     * 获取近7天推理趋势
     */
    public List<WeeklyTrendVO> getWeeklyTrend() {
        List<WeeklyTrendVO> trendList = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            LocalDateTime dayStart = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime dayEnd = LocalDateTime.of(date, LocalTime.MAX);

            Long count = inferenceMapper.countByTimeRange(dayStart, dayEnd);

            LambdaQueryWrapper<Alert> alertDayWrapper = new LambdaQueryWrapper<>();
            alertDayWrapper.ge(Alert::getAlertTime, dayStart).le(Alert::getAlertTime, dayEnd);
            Long alertCount = alertMapper.selectCount(alertDayWrapper);

            WeeklyTrendVO vo = new WeeklyTrendVO();
            vo.setDate(date.format(formatter));
            vo.setCount(count);
            vo.setAlertCount(alertCount);
            trendList.add(vo);
        }

        return trendList;
    }

    /**
     * 获取按业务线告警排名
     */
    public List<AlertRankingVO> getAlertRanking() {
        List<AlertRankingVO> rankingList = new ArrayList<>();

        // 这里简化处理，实际需要通过SQL聚合查询
        LambdaQueryWrapper<Alert> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Alert::getAlertTime);
        wrapper.last("LIMIT 100");
        List<Alert> alerts = alertMapper.selectList(wrapper);

        // 简单统计（实际应该用GROUP BY）
        java.util.Map<String, Long> businessCountMap = new java.util.HashMap<>();
        for (Alert alert : alerts) {
            String businessLine = "default"; // 实际应该从camera或inference记录获取
            businessCountMap.put(businessLine, businessCountMap.getOrDefault(businessLine, 0L) + 1);
        }

        for (java.util.Map.Entry<String, Long> entry : businessCountMap.entrySet()) {
            AlertRankingVO vo = new AlertRankingVO();
            vo.setBusinessLine(entry.getKey());
            vo.setCount(entry.getValue());
            rankingList.add(vo);
        }

        // 按数量降序排序
        rankingList.sort((a, b) -> Long.compare(b.getCount(), a.getCount()));

        return rankingList;
    }

    /**
     * 获取实时告警列表
     */
    public List<AlertVO> getRealtimeAlerts(Integer limit) {
        return alertService.getLatestAlerts(limit);
    }

    /**
     * 获取系统健康概览
     */
    public SystemHealthVO getSystemHealth() {
        SystemHealthVO vo = new SystemHealthVO();

        // 节点统计
        vo.setTotalNodeCount(inferenceNodeMapper.selectCount(null));
        LambdaQueryWrapper<InferenceNode> onlineWrapper = new LambdaQueryWrapper<>();
        onlineWrapper.eq(InferenceNode::getStatus, "online");
        vo.setOnlineNodeCount(inferenceNodeMapper.selectCount(onlineWrapper));

        // 模型统计
        vo.setTotalModelCount(modelMapper.selectCount(null));
        LambdaQueryWrapper<ModelNodeDeployment> deployedWrapper = new LambdaQueryWrapper<>();
        deployedWrapper.eq(ModelNodeDeployment::getStatus, "loaded");
        vo.setDeployedModelCount(modelNodeDeploymentMapper.selectCount(deployedWrapper));

        // 任务统计
        vo.setRunningTaskCount(monitorTaskMapper.selectCount(
                new LambdaQueryWrapper<MonitorTask>().eq(MonitorTask::getStatus, "running")));
        vo.setStoppedTaskCount(monitorTaskMapper.selectCount(
                new LambdaQueryWrapper<MonitorTask>().eq(MonitorTask::getStatus, "stopped")));
        vo.setErrorTaskCount(monitorTaskMapper.selectCount(
                new LambdaQueryWrapper<MonitorTask>().eq(MonitorTask::getStatus, "error")));

        // 未读告警
        LambdaQueryWrapper<Alert> unreadWrapper = new LambdaQueryWrapper<>();
        unreadWrapper.eq(Alert::getReadStatus, false);
        vo.setUnreadAlertCount(alertMapper.selectCount(unreadWrapper));

        return vo;
    }
}
