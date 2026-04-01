package com.vision.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 模型节点部署实体
 * 对应表: model_node_deployment
 * 记录模型在哪些推理节点上已加载
 */
@Data
@TableName("model_node_deployment")
public class ModelNodeDeployment {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /**
     * 模型ID
     */
    private String modelId;

    /**
     * 推理节点ID
     */
    private String nodeId;

    /**
     * 设备类型: cpu/cuda
     */
    private String device;

    /**
     * 设备名称，如 NVIDIA GeForce RTX 3060
     */
    private String deviceName;

    /**
     * 部署状态: loading/loaded/error
     */
    private String status;

    /**
     * 部署时间
     */
    private LocalDateTime deployedAt;
}
