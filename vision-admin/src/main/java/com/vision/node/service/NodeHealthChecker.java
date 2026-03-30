package com.vision.node.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.vision.node.entity.InferenceNode;
import com.vision.node.mapper.InferenceNodeMapper;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class NodeHealthChecker {

    private final InferenceNodeMapper nodeMapper;
    private final NodeService nodeService;
    private final NodeFailoverService failoverService;

    @Value("${vision.inference.heartbeat-timeout:60}")
    private int heartbeatTimeoutSeconds;

    /**
     * 每 30 秒检查一次在线节点的心跳
     */
    @Scheduled(fixedRateString = "${vision.inference.health-check-interval:30}000")
    public void checkNodes() {
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
