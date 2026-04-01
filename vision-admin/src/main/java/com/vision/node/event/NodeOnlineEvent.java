package com.vision.node.event;

import org.springframework.context.ApplicationEvent;

/**
 * 节点上线事件
 */
public class NodeOnlineEvent extends ApplicationEvent {

    private final String nodeId;

    public NodeOnlineEvent(Object source, String nodeId) {
        super(source);
        this.nodeId = nodeId;
    }

    public String getNodeId() {
        return nodeId;
    }
}
