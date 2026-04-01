package com.vision.node.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.vision.node.entity.InferenceNode;
import com.vision.node.mapper.InferenceNodeMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 节点健康检查调度器
 * 定时检测心跳超时的节点并触发故障迁移
 */
@Slf4j
@Component
public class NodeHealthChecker {

    private final InferenceNodeMapper nodeMapper;
    private final NodeService nodeService;
    private final NodeFailoverService failoverService;

    @Value("${vision.inference.heartbeat-timeout:60}")
    private int heartbeatTimeoutSeconds;

    /** 服务启动时间，用于启动后的宽限期判断 */
    private final LocalDateTime startupTime = LocalDateTime.now();
    private boolean gracePeriodLogged = false;

    public NodeHealthChecker(InferenceNodeMapper nodeMapper, NodeService nodeService,
                             NodeFailoverService failoverService) {
        this.nodeMapper = nodeMapper;
        this.nodeService = nodeService;
        this.failoverService = failoverService;
    }

    /**
     * 每 30 秒检查一次在线节点的心跳。
     * 启动后有一段宽限期（heartbeatTimeout × 2），等待推理节点重新发送心跳，
     * 避免因 Java 停机导致心跳时间戳过期而误判节点离线。
     */
    @Scheduled(fixedRateString = "${vision.inference.health-check-interval:30}000")
    public void checkNodes() {
        // 启动宽限期：等待节点心跳恢复
        long gracePeriodSeconds = heartbeatTimeoutSeconds * 2L;
        if (LocalDateTime.now().isBefore(startupTime.plusSeconds(gracePeriodSeconds))) {
            if (!gracePeriodLogged) {
                log.info("服务刚启动，健康检查进入宽限期（{}秒），等待推理节点心跳恢复", gracePeriodSeconds);
                gracePeriodLogged = true;
            }
            return;
        }

        LambdaQueryWrapper<InferenceNode> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InferenceNode::getStatus, "online");

        List<InferenceNode> onlineNodes = nodeMapper.selectList(wrapper);
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(heartbeatTimeoutSeconds);

        for (InferenceNode node : onlineNodes) {
            if (node.getLastHeartbeat() != null && node.getLastHeartbeat().isBefore(threshold)) {
                log.warn("节点心跳超时: id={}, name={}, lastHeartbeat={}",
                        node.getId(), node.getNodeName(), node.getLastHeartbeat());
                nodeService.markOffline(node.getId());
                failoverService.handleNodeOffline(node.getId());
            }
        }
    }
}
