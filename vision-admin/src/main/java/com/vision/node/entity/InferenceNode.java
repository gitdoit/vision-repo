package com.vision.node.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 推理节点实体
 * 对应表: inference_node
 */
@Data
@TableName("inference_node")
public class InferenceNode {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /** 节点名称 */
    private String nodeName;

    /** 节点地址（IP 或域名） */
    private String host;

    /** 服务端口 */
    private Integer port;

    /** 状态: online / offline / unknown */
    private String status;

    /** 设备类型: cpu / cuda */
    private String deviceType;

    /** GPU 型号 */
    private String gpuName;

    /** GPU 数量 */
    private Integer gpuCount;

    /** CPU 信息 */
    private String cpuInfo;

    /** 总内存（字节） */
    private Long memoryTotal;

    /** 最后心跳时间 */
    private LocalDateTime lastHeartbeat;

    /** 注册时间 */
    private LocalDateTime registeredAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
