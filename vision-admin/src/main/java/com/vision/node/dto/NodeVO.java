package com.vision.node.dto;

import com.vision.node.entity.InferenceNode;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 节点响应 VO
 */
@Data
public class NodeVO {

    private String id;
    private String nodeName;
    private String host;
    private Integer port;
    private String status;
    private String deviceType;
    private String gpuName;
    private Integer gpuCount;
    private String cpuInfo;
    private Long memoryTotal;
    private LocalDateTime lastHeartbeat;
    private LocalDateTime registeredAt;

    /** 运行时信息（来自心跳缓存） */
    private NodeRuntimeInfo runtimeInfo;

    public static NodeVO fromEntity(InferenceNode entity) {
        NodeVO vo = new NodeVO();
        vo.setId(entity.getId());
        vo.setNodeName(entity.getNodeName());
        vo.setHost(entity.getHost());
        vo.setPort(entity.getPort());
        vo.setStatus(entity.getStatus());
        vo.setDeviceType(entity.getDeviceType());
        vo.setGpuName(entity.getGpuName());
        vo.setGpuCount(entity.getGpuCount());
        vo.setCpuInfo(entity.getCpuInfo());
        vo.setMemoryTotal(entity.getMemoryTotal());
        vo.setLastHeartbeat(entity.getLastHeartbeat());
        vo.setRegisteredAt(entity.getRegisteredAt());
        return vo;
    }
}
