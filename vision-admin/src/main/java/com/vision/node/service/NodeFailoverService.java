package com.vision.node.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.vision.model.entity.ModelNodeDeployment;
import com.vision.model.mapper.ModelNodeDeploymentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 节点故障迁移服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NodeFailoverService {

    private final ModelNodeDeploymentMapper deploymentMapper;

    /**
     * 处理节点离线：删除该节点上所有部署记录
     */
    @Transactional(rollbackFor = Exception.class)
    public void handleNodeOffline(String nodeId) {
        LambdaQueryWrapper<ModelNodeDeployment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ModelNodeDeployment::getNodeId, nodeId);

        List<ModelNodeDeployment> affected = deploymentMapper.selectList(wrapper);

        for (ModelNodeDeployment d : affected) {
            deploymentMapper.deleteById(d.getId());
            log.warn("节点离线，部署已移除: modelId={}, nodeId={}", d.getModelId(), nodeId);
        }

        if (!affected.isEmpty()) {
            log.warn("节点 {} 离线，共 {} 个模型部署已移除", nodeId, affected.size());
        }
    }
}
