package com.vision.task.service;

import com.vision.camera.entity.Camera;
import com.vision.camera.mapper.CameraMapper;
import com.vision.model.entity.Model;
import com.vision.model.mapper.ModelMapper;
import com.vision.task.entity.MonitorTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 监测任务调度器
 *
 * 职责:
 * - 定时扫描运行中的监测任务
 * - 检查时间窗口（生效时段、星期、日期范围）
 * - 展开分组 → 摄像头列表
 * - 按任务级 captureFrequency 检查频率
 * - 提交异步推理管道执行
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskScheduler {

    private final MonitorTaskService monitorTaskService;
    private final CameraMapper cameraMapper;
    private final ModelMapper modelMapper;
    private final TaskInferencePipeline taskInferencePipeline;
    private final TaskConditionEvaluator conditionEvaluator;

    /**
     * 记录每个 taskId:cameraId 最后一次调度时间，用于判断频率
     */
    private final Map<String, LocalDateTime> lastDispatchTime = new ConcurrentHashMap<>();

    /**
     * 定时扫描运行中的监测任务
     * 每 30 秒执行一次
     */
    @Scheduled(fixedDelay = 30000, initialDelay = 15000)
    public void scheduleTasks() {
        try {
            List<MonitorTask> runningTasks = monitorTaskService.getRunningTasks();

            if (runningTasks.isEmpty()) {
                log.debug("无运行中的监测任务");
                return;
            }

            log.debug("扫描运行中的监测任务: count={}", runningTasks.size());
            LocalDateTime now = LocalDateTime.now();

            for (MonitorTask task : runningTasks) {
                try {
                    processTask(task, now);
                } catch (Exception e) {
                    log.error("处理监测任务异常: taskId={}, taskName={}", task.getId(), task.getName(), e);
                }
            }

        } catch (Exception e) {
            log.error("任务调度异常", e);
        }
    }

    /**
     * 处理单个任务
     */
    private void processTask(MonitorTask task, LocalDateTime now) {
        String taskId = task.getId();

        // 1. 检查时间窗口
        if (!isWithinTimeWindow(task, now)) {
            log.debug("任务不在生效时间窗口内: taskId={}", taskId);
            return;
        }

        // 2. 校验模型仍然已加载
        Model model = modelMapper.selectById(task.getModelId());
        if (model == null || !"loaded".equals(model.getStatus())) {
            log.warn("任务关联模型未加载，跳过: taskId={}, modelId={}", taskId, task.getModelId());
            return;
        }

        // 3. 展开分组获取摄像头列表
        List<String> cameraIds = cameraMapper.selectCameraIdsByGroupIds(List.of(task.getGroupId()));
        if (cameraIds == null || cameraIds.isEmpty()) {
            log.debug("任务分组下无摄像头: taskId={}, groupId={}", taskId, task.getGroupId());
            return;
        }

        // 4. 解析任务抓图频率
        int frequencySeconds = parseFrequencySeconds(task.getCaptureFrequency());

        // 5. 遍历摄像头，按频率提交
        for (String cameraId : cameraIds) {
            try {
                if (shouldDispatch(taskId, cameraId, frequencySeconds, now)) {
                    Camera camera = cameraMapper.selectById(cameraId);
                    if (camera == null || !"online".equals(camera.getStatus())) {
                        log.debug("摄像头不在线或不存在，跳过: cameraId={}", cameraId);
                        continue;
                    }

                    // 记录调度时间
                    lastDispatchTime.put(taskId + ":" + cameraId, now);

                    // 异步执行推理管道
                    executeAsync(task, camera, model);
                }
            } catch (Exception e) {
                log.error("调度任务摄像头异常: taskId={}, cameraId={}", taskId, cameraId, e);
            }
        }
    }

    /**
     * 异步执行推理管道
     */
    @Async("captureExecutor")
    public void executeAsync(MonitorTask task, Camera camera, Model model) {
        taskInferencePipeline.execute(task, camera, model);
    }

    /**
     * 检查是否在生效时间窗口内
     */
    private boolean isWithinTimeWindow(MonitorTask task, LocalDateTime now) {
        // 日期范围检查
        LocalDate today = now.toLocalDate();
        if (task.getEffectiveStart() != null && today.isBefore(task.getEffectiveStart())) {
            return false;
        }
        if (task.getEffectiveEnd() != null && today.isAfter(task.getEffectiveEnd())) {
            return false;
        }

        // 星期检查
        if (task.getScheduleWeekdays() != null && !task.getScheduleWeekdays().isBlank()) {
            int dayOfWeek = now.getDayOfWeek().getValue(); // 1=Monday, 7=Sunday
            Set<Integer> allowedDays = parseWeekdays(task.getScheduleWeekdays());
            if (!allowedDays.contains(dayOfWeek)) {
                return false;
            }
        }

        // 时段检查
        if (task.getScheduleStartTime() != null && task.getScheduleEndTime() != null) {
            LocalTime currentTime = now.toLocalTime();
            LocalTime startTime = LocalTime.parse(task.getScheduleStartTime(), DateTimeFormatter.ofPattern("HH:mm"));
            LocalTime endTime = LocalTime.parse(task.getScheduleEndTime(), DateTimeFormatter.ofPattern("HH:mm"));

            if (startTime.isBefore(endTime)) {
                // 正常时段: 08:00 - 22:00
                if (currentTime.isBefore(startTime) || currentTime.isAfter(endTime)) {
                    return false;
                }
            } else {
                // 跨午夜时段: 22:00 - 06:00
                if (currentTime.isAfter(endTime) && currentTime.isBefore(startTime)) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * 解析星期字符串为集合
     * 输入格式: "1,2,3,4,5"
     */
    private Set<Integer> parseWeekdays(String weekdays) {
        Set<Integer> result = new HashSet<>();
        for (String s : weekdays.split(",")) {
            try {
                result.add(Integer.parseInt(s.trim()));
            } catch (NumberFormatException e) {
                log.warn("无法解析星期: {}", s);
            }
        }
        return result;
    }

    /**
     * 判断是否该调度（检查频率）
     */
    private boolean shouldDispatch(String taskId, String cameraId, int frequencySeconds, LocalDateTime now) {
        String key = taskId + ":" + cameraId;
        LocalDateTime lastTime = lastDispatchTime.get(key);

        if (lastTime == null) {
            return true;
        }

        long elapsed = Duration.between(lastTime, now).getSeconds();
        return elapsed >= frequencySeconds;
    }

    /**
     * 解析抓图频率字符串为秒数
     * 复用 CaptureScheduler 的格式: 1s, 5s, 1min, 5min, 1h
     */
    private int parseFrequencySeconds(String frequency) {
        if (frequency == null || frequency.isBlank()) {
            return 300; // 默认 5 分钟
        }

        frequency = frequency.toLowerCase().trim();

        try {
            return Integer.parseInt(frequency);
        } catch (NumberFormatException e) {
            // 继续处理带单位格式
        }

        if (frequency.endsWith("sec")) {
            return Integer.parseInt(frequency.replace("sec", "").trim());
        } else if (frequency.endsWith("s")) {
            return Integer.parseInt(frequency.replace("s", "").trim());
        } else if (frequency.endsWith("min")) {
            return Integer.parseInt(frequency.replace("min", "").trim()) * 60;
        } else if (frequency.endsWith("h")) {
            return Integer.parseInt(frequency.replace("h", "").trim()) * 3600;
        }

        log.warn("无法解析抓图频率: {}, 使用默认值 5 分钟", frequency);
        return 300;
    }

    /**
     * 清理已停止任务的调度记录
     */
    public void clearTaskDispatchHistory(String taskId) {
        lastDispatchTime.keySet().removeIf(key -> key.startsWith(taskId + ":"));
        conditionEvaluator.clearTaskCache(taskId);
    }
}
