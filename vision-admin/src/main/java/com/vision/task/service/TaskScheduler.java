package com.vision.task.service;

import com.vision.camera.entity.Camera;
import com.vision.camera.mapper.CameraMapper;
import com.vision.model.entity.Model;
import com.vision.model.entity.ModelNodeDeployment;
import com.vision.model.mapper.ModelMapper;
import com.vision.model.service.ModelService;
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
 * - 触发 CaptureCoordinator 重算截帧频率
 * - 检查时间窗口（生效时段、星期、日期范围）
 * - 从 CaptureCoordinator 帧缓冲消费预截帧结果
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
    private final ModelService modelService;
    private final TaskInferencePipeline taskInferencePipeline;
    private final TaskConditionEvaluator conditionEvaluator;
    private final CaptureCoordinator captureCoordinator;

    /**
     * 记录每个 taskId:cameraId 最后一次调度时间，用于判断频率
     */
    private final Map<String, LocalDateTime> lastDispatchTime = new ConcurrentHashMap<>();

    /**
     * 记录每个 taskId:cameraId 最后消费的 frameId，防止同帧重复消费
     */
    private final Map<String, String> lastConsumedFrameId = new ConcurrentHashMap<>();

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

            // 触发预截帧频率重算
            captureCoordinator.recalculateFrequencies(runningTasks);

            log.info("任务调度扫描: 运行中任务数={}", runningTasks.size());
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

        // 2. 校验模型至少在一个节点上加载
        Model model = modelMapper.selectById(task.getModelId());
        if (model == null) {
            log.warn("任务关联模型不存在，跳过: taskId={}, modelId={}", taskId, task.getModelId());
            return;
        }
        ModelNodeDeployment deployment = modelService.findLoadedDeployment(model.getId());
        if (deployment == null) {
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
        int frequencySeconds = CaptureCoordinator.parseFrequencySeconds(task.getCaptureFrequency());

        // 5. 遍历摄像头，消费预截帧结果并提交推理
        for (String cameraId : cameraIds) {
            try {
                if (!shouldDispatch(taskId, cameraId, frequencySeconds, now)) {
                    continue;
                }

                // 从帧缓冲获取最新帧
                CaptureFrame frame = captureCoordinator.getLatestFrame(cameraId);
                if (frame == null || !frame.isSuccess()) {
                    log.debug("无可用帧，跳过: taskId={}, cameraId={}", taskId, cameraId);
                    continue;
                }

                // 校验帧新鲜度: 帧龄不超过任务频率的 1.5 倍
                long frameAgeSeconds = Duration.between(frame.getCaptureTime(), now).getSeconds();
                long maxAgeSeconds = (long) (frequencySeconds * 1.5);
                if (frameAgeSeconds > maxAgeSeconds) {
                    log.debug("帧已过期，跳过: taskId={}, cameraId={}, 帧龄={}s, 上限={}s",
                            taskId, cameraId, frameAgeSeconds, maxAgeSeconds);
                    continue;
                }

                // 校验帧唯一性: 同一帧不被同一任务重复消费
                String dispatchKey = taskId + ":" + cameraId;
                String lastFrameId = lastConsumedFrameId.get(dispatchKey);
                if (frame.getFrameId().equals(lastFrameId)) {
                    log.debug("帧已消费，跳过: taskId={}, cameraId={}, frameId={}", taskId, cameraId, frame.getFrameId());
                    continue;
                }

                Camera camera = cameraMapper.selectById(cameraId);
                if (camera == null) {
                    log.debug("摄像头不存在，跳过: cameraId={}", cameraId);
                    continue;
                }

                // 记录调度时间和消费帧ID
                lastDispatchTime.put(dispatchKey, now);
                lastConsumedFrameId.put(dispatchKey, frame.getFrameId());

                log.info("调度推理: taskId={}, taskName={}, cameraId={}, cameraName={}, frameId={}",
                        taskId, task.getName(), cameraId, camera.getName(), frame.getFrameId());

                // 异步执行推理管道（使用预截帧结果）
                executeAsync(task, camera, model, frame);

            } catch (Exception e) {
                log.error("调度任务摄像头异常: taskId={}, cameraId={}", taskId, cameraId, e);
            }
        }
    }

    /**
     * 异步执行推理管道
     */
    @Async("captureExecutor")
    public void executeAsync(MonitorTask task, Camera camera, Model model, CaptureFrame frame) {
        taskInferencePipeline.execute(task, camera, model, frame);
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
     * 委托给 CaptureCoordinator 的静态方法
     */
    private int parseFrequencySeconds(String frequency) {
        return CaptureCoordinator.parseFrequencySeconds(frequency);
    }

    /**
     * 清理已停止任务的调度记录
     */
    public void clearTaskDispatchHistory(String taskId) {
        lastDispatchTime.keySet().removeIf(key -> key.startsWith(taskId + ":"));
        lastConsumedFrameId.keySet().removeIf(key -> key.startsWith(taskId + ":"));
        conditionEvaluator.clearTaskCache(taskId);
    }
}
