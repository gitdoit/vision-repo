package com.vision.node.service;

import com.vision.common.exception.BizException;
import com.vision.node.dto.NodeRuntimeInfo;
import com.vision.node.dto.NodeVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 节点路由器 — 为推理请求选择目标节点
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NodeRouter {

    private final NodeService nodeService;

    /**
     * 选择加载模型的目标节点
     *
     * @param preferredNodeId 用户指定的节点（可为 null 表示自动选择）
     * @return 目标节点 ID
     */
    public String selectNodeForLoad(String preferredNodeId) {
        if (preferredNodeId != null && !preferredNodeId.isBlank()) {
            // 验证指定节点在线
            NodeVO node = nodeService.getNodeById(preferredNodeId);
            if (!"online".equals(node.getStatus())) {
                throw new BizException("指定节点不在线: " + node.getNodeName());
            }
            return preferredNodeId;
        }

        // 自动选择：负载最低的在线节点
        List<NodeVO> onlineNodes = nodeService.getOnlineNodes();
        if (onlineNodes.isEmpty()) {
            throw new BizException("没有可用的在线推理节点");
        }

        // 按 CPU 使用率排序，选择最空闲的
        return onlineNodes.stream()
                .min(Comparator.comparingDouble(this::getLoadScore))
                .map(NodeVO::getId)
                .orElse(onlineNodes.get(0).getId());
    }

    /**
     * 获取节点的 HTTP 基础地址
     */
    public String getNodeUrl(String nodeId) {
        NodeVO node = nodeService.getNodeById(nodeId);
        return "http://" + node.getHost() + ":" + node.getPort();
    }

    /**
     * 计算节点负载分数（越低越好）
     */
    private double getLoadScore(NodeVO node) {
        NodeRuntimeInfo info = node.getRuntimeInfo();
        if (info == null || info.getSystemLoad() == null) {
            return 0.0; // 无数据视为空闲
        }

        Map<String, Object> load = info.getSystemLoad();
        double cpu = toDouble(load.get("cpuPercent"));
        double mem = toDouble(load.get("memoryPercent"));
        double gpu = toDouble(load.get("gpuPercent"));

        // 加权平均：GPU 权重最高
        return cpu * 0.3 + mem * 0.2 + gpu * 0.5;
    }

    private double toDouble(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return 0.0;
    }
}
