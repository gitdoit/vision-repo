package com.vision.task.dto;

import lombok.Data;

/**
 * 任务关联的推理节点简要信息
 */
@Data
public class TaskNodeInfo {

    private String nodeId;

    private String nodeName;

    private String host;

    private Integer port;

    private String status;
}
