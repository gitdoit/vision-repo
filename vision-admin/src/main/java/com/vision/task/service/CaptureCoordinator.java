package com.vision.task.service;

import com.vision.camera.mapper.CameraMapper;
import com.vision.task.entity.MonitorTask;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 预截帧协调器
 *
 * 职责:
 * - 维护每个摄像头的最新帧缓冲（仅保留最新一帧）
 * - 根据运行中任务动态计算每个摄像头的截帧频率（取所有关联任务的最小频率）
 * - 跟踪每个摄像头的最后截帧时间，判断是否到期
 * - 维护摄像头健康状态，实现截帧失败退避机制
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CaptureCoordinator {

    private final CameraMapper cameraMapper;

    /** 最新帧缓冲: cameraId → CaptureFrame */
    private final Map<String, CaptureFrame> frameBuffer = new ConcurrentHashMap<>();

    /** 截帧频率表: cameraId → 秒数（取所有关联任务的最小频率） */
    private final Map<String, Integer> captureFrequencies = new ConcurrentHashMap<>();

    /** 最后截帧时间: cameraId → LocalDateTime */
    private final Map<String, LocalDateTime> lastCaptureTime = new ConcurrentHashMap<>();

    /** 摄像头健康状态: cameraId → CameraHealthState */
    private final Map<String, CameraHealthState> healthStates = new ConcurrentHashMap<>();

    /** 退避时间阶梯（分钟）: 5min → 15min → 30min */
    private static final int[] BACKOFF_MINUTES = {5, 15, 30};

    /** 连续失败多少次后标记不可用 */
    private static final int FAILURE_THRESHOLD = 3;

    // ==================== 帧缓冲操作 ====================

    /**
     * 存入最新帧
     */
    public void putFrame(CaptureFrame frame) {
        frameBuffer.put(frame.getCameraId(), frame);
        lastCaptureTime.put(frame.getCameraId(), frame.getCaptureTime());
    }

    /**
     * 获取摄像头最新帧（任务推理层消费）
     */
    public CaptureFrame getLatestFrame(String cameraId) {
        return frameBuffer.get(cameraId);
    }

    // ==================== 频率管理 ====================

    /**
     * 根据运行中任务重算所有摄像头的截帧频率
     *
     * 逻辑: 遍历所有运行中任务 → 展开分组获取摄像头 → 对每个摄像头取所有关联任务的最小频率
     */
    public void recalculateFrequencies(List<MonitorTask> runningTasks) {
        // 新频率表
        Map<String, Integer> newFrequencies = new HashMap<>();

        for (MonitorTask task : runningTasks) {
            int freq = parseFrequencySeconds(task.getCaptureFrequency());
            List<String> cameraIds = cameraMapper.selectCameraIdsByGroupIds(List.of(task.getGroupId()));
            if (cameraIds == null) continue;

            for (String cameraId : cameraIds) {
                newFrequencies.merge(cameraId, freq, Math::min);
            }
        }

        // 找出被移除的摄像头（不再被任何运行中任务引用）
        Set<String> removed = new HashSet<>(captureFrequencies.keySet());
        removed.removeAll(newFrequencies.keySet());

        // 清理被移除摄像头的状态
        for (String cameraId : removed) {
            frameBuffer.remove(cameraId);
            lastCaptureTime.remove(cameraId);
            healthStates.remove(cameraId);
        }

        captureFrequencies.clear();
        captureFrequencies.putAll(newFrequencies);

        log.debug("截帧频率重算完成: 摄像头数={}, 移除={}", newFrequencies.size(), removed.size());
    }

    /**
     * 获取所有需要截帧的摄像头 ID 及其频率
     */
    public Map<String, Integer> getCaptureFrequencies() {
        return Collections.unmodifiableMap(captureFrequencies);
    }

    /**
     * 判断摄像头是否到期需要截帧
     */
    public boolean isDueForCapture(String cameraId, LocalDateTime now) {
        Integer freq = captureFrequencies.get(cameraId);
        if (freq == null) return false;

        LocalDateTime lastTime = lastCaptureTime.get(cameraId);
        if (lastTime == null) return true;

        long elapsed = Duration.between(lastTime, now).getSeconds();
        return elapsed >= freq;
    }

    // ==================== 健康状态与退避 ====================

    /**
     * 记录截帧成功，重置健康状态
     */
    public void recordSuccess(String cameraId) {
        CameraHealthState state = healthStates.get(cameraId);
        if (state != null) {
            if (state.isUnavailable()) {
                log.info("摄像头恢复可用: cameraId={}, 不可用时长={}min",
                        cameraId, Duration.between(state.getUnavailableSince(), LocalDateTime.now()).toMinutes());
            }
            healthStates.remove(cameraId);
        }
    }

    /**
     * 记录截帧失败，可能触发退避机制
     */
    public void recordFailure(String cameraId) {
        CameraHealthState state = healthStates.computeIfAbsent(cameraId, k -> new CameraHealthState());
        state.setConsecutiveFailures(state.getConsecutiveFailures() + 1);

        if (!state.isUnavailable() && state.getConsecutiveFailures() >= FAILURE_THRESHOLD) {
            // 进入退避状态
            state.setUnavailable(true);
            state.setUnavailableSince(LocalDateTime.now());
            state.setBackoffLevel(0);
            state.setNextRetryTime(LocalDateTime.now().plusMinutes(BACKOFF_MINUTES[0]));
            log.warn("摄像头标记不可用（连续{}次截帧失败）: cameraId={}, 下次重试={}",
                    state.getConsecutiveFailures(), cameraId, state.getNextRetryTime());
        }
    }

    /**
     * 退避重试失败，升级退避等级
     */
    public void escalateBackoff(String cameraId) {
        CameraHealthState state = healthStates.get(cameraId);
        if (state == null || !state.isUnavailable()) return;

        int newLevel = Math.min(state.getBackoffLevel() + 1, BACKOFF_MINUTES.length - 1);
        state.setBackoffLevel(newLevel);
        state.setNextRetryTime(LocalDateTime.now().plusMinutes(BACKOFF_MINUTES[newLevel]));
        state.setConsecutiveFailures(state.getConsecutiveFailures() + 1);

        log.warn("摄像头退避升级: cameraId={}, level={}, 退避{}min, 下次重试={}",
                cameraId, newLevel, BACKOFF_MINUTES[newLevel], state.getNextRetryTime());
    }

    /**
     * 判断摄像头是否处于退避中且未到重试时间
     *
     * @return true=应该跳过截帧, false=可以截帧（正常或到了重试窗口）
     */
    public boolean shouldSkipCapture(String cameraId, LocalDateTime now) {
        CameraHealthState state = healthStates.get(cameraId);
        if (state == null || !state.isUnavailable()) {
            return false; // 正常状态，不跳过
        }
        // 未到重试时间 → 跳过
        return now.isBefore(state.getNextRetryTime());
    }

    /**
     * 判断摄像头当前是否处于退避重试窗口（不可用但到了重试时间）
     */
    public boolean isInRetryWindow(String cameraId, LocalDateTime now) {
        CameraHealthState state = healthStates.get(cameraId);
        if (state == null || !state.isUnavailable()) {
            return false;
        }
        return !now.isBefore(state.getNextRetryTime());
    }

    /**
     * 清理指定摄像头的所有状态
     */
    public void clearCamera(String cameraId) {
        frameBuffer.remove(cameraId);
        captureFrequencies.remove(cameraId);
        lastCaptureTime.remove(cameraId);
        healthStates.remove(cameraId);
    }

    // ==================== 工具方法 ====================

    /**
     * 解析抓图频率字符串为秒数
     */
    static int parseFrequencySeconds(String frequency) {
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

        return 300;
    }

    // ==================== 内部类 ====================

    @Data
    static class CameraHealthState {
        /** 连续失败次数 */
        private int consecutiveFailures;
        /** 是否标记为不可用 */
        private boolean unavailable;
        /** 不可用开始时间 */
        private LocalDateTime unavailableSince;
        /** 退避等级（0=5min, 1=15min, 2=30min） */
        private int backoffLevel;
        /** 下次重试时间 */
        private LocalDateTime nextRetryTime;
    }
}
