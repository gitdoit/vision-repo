package com.vision.node.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.vision.model.entity.Model;
import com.vision.model.mapper.ModelMapper;
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

    private final ModelMapper modelMapper;

    /**
     * 处理节点离线：将节点上的模型标记为 unloaded
     */
    @Transactional(rollbackFor = Exception.class)
    public void handleNodeOffline(String nodeId) {
        LambdaQueryWrapper<Model> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Model::getNodeId, nodeId)
               .eq(Model::getStatus, "loaded");

        List<Model> affectedModels = modelMapper.selectList(wrapper);

        for (Model model : affectedModels) {
            model.setStatus("unloaded");
            model.setNodeId(null);
            model.setDevice(null);
            model.setDeviceName(null);
            modelMapper.updateById(model);
            log.warn("节点离线，模型已卸载: modelId={}, modelName={}, nodeId={}", model.getId(), model.getName(), nodeId);
        }

        if (!affectedModels.isEmpty()) {
            log.warn("节点 {} 离线，共 {} 个模型已标记为 unloaded", nodeId, affectedModels.size());
        }
    }
}
