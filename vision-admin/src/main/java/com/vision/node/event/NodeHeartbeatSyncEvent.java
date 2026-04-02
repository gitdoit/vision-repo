package com.vision.node.event;

import org.springframework.context.ApplicationEvent;

import java.util.List;
import java.util.Map;

/**
 * 节点心跳同步事件 — 用于将心跳上报的已加载模型列表同步到 model_node_deployment 表
 */
public class NodeHeartbeatSyncEvent extends ApplicationEvent {

    private final String nodeId;
    private final List<Map<String, Object>> loadedModels;

    public NodeHeartbeatSyncEvent(Object source, String nodeId, List<Map<String, Object>> loadedModels) {
        super(source);
        this.nodeId = nodeId;
        this.loadedModels = loadedModels;
    }

    public String getNodeId() {
        return nodeId;
    }

    public List<Map<String, Object>> getLoadedModels() {
        return loadedModels;
    }
}
