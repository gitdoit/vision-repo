package com.vision.node.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 节点运行时信息（存活在 Caffeine 缓存中，由心跳更新）
 */
@Data
public class NodeRuntimeInfo {

    /** 已加载模型列表 */
    private List<Map<String, Object>> loadedModels;

    /** 活跃流任务列表 */
    private List<Map<String, Object>> activeTasks;

    /** 系统负载: cpuPercent, memoryPercent, gpuPercent */
    private Map<String, Object> systemLoad;
}
