package com.vision.node.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 节点注册请求 DTO
 */
@Data
public class NodeRegisterDTO {

    /** 节点 ID（重启后沿用，首次为空） */
    private String nodeId;

    /** 节点名称 */
    @NotBlank(message = "节点名称不能为空")
    private String nodeName;

    /** 节点可达地址 */
    @NotBlank(message = "节点地址不能为空")
    private String host;

    /** 服务端口 */
    @NotNull(message = "端口不能为空")
    private Integer port;

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
}
