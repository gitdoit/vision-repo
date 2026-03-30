package com.vision.node.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 节点心跳 DTO
 */
@Data
public class NodeHeartbeatDTO {

    @NotBlank(message = "nodeId 不能为空")
    private String nodeId;

    /** 已加载模型列表 */
    private List<Map<String, Object>> loadedModels;

    /** 活跃流任务列表 */
    private List<Map<String, Object>> activeTasks;

    /** 系统负载 */
    private Map<String, Object> systemLoad;
}
