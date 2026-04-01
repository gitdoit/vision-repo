package com.vision.model.dto;

import com.vision.model.entity.ModelNodeDeployment;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 模型部署信息 VO（展示模型在某个节点的加载状态）
 */
@Data
public class ModelDeploymentVO {

    private String id;
    private String nodeId;
    private String nodeName;
    private String device;
    private String deviceName;
    private String status;
    private LocalDateTime deployedAt;

    public static ModelDeploymentVO fromEntity(ModelNodeDeployment entity) {
        ModelDeploymentVO vo = new ModelDeploymentVO();
        vo.setId(entity.getId());
        vo.setNodeId(entity.getNodeId());
        vo.setDevice(entity.getDevice());
        vo.setDeviceName(entity.getDeviceName());
        vo.setStatus(entity.getStatus());
        vo.setDeployedAt(entity.getDeployedAt());
        return vo;
    }
}
