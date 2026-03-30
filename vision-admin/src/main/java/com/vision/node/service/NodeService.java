package com.vision.node.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.vision.common.exception.BizException;
import com.vision.common.util.IdUtil;
import com.vision.node.dto.*;
import com.vision.node.entity.InferenceNode;
import com.vision.node.mapper.InferenceNodeMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 节点注册与管理服务
 */
@Slf4j
@Service
public class NodeService {

    private final InferenceNodeMapper nodeMapper;

    /** 运行时信息缓存: nodeId → NodeRuntimeInfo, TTL 60s */
    private final Cache<String, NodeRuntimeInfo> runtimeCache;

    public NodeService(InferenceNodeMapper nodeMapper) {
        this.nodeMapper = nodeMapper;
        this.runtimeCache = Caffeine.newBuilder()
                .expireAfterWrite(60, TimeUnit.SECONDS)
                .maximumSize(100)
                .build();
    }

    /**
     * 节点注册（幂等：按 host:port 去重）
     */
    @Transactional(rollbackFor = Exception.class)
    public NodeVO register(NodeRegisterDTO dto) {
        // 按 host:port 查找已有节点
        LambdaQueryWrapper<InferenceNode> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InferenceNode::getHost, dto.getHost())
               .eq(InferenceNode::getPort, dto.getPort());

        InferenceNode existing = nodeMapper.selectOne(wrapper);

        if (existing != null) {
            // 已存在：更新信息并上线
            existing.setNodeName(dto.getNodeName());
            existing.setDeviceType(dto.getDeviceType());
            existing.setGpuName(dto.getGpuName());
            existing.setGpuCount(dto.getGpuCount());
            existing.setCpuInfo(dto.getCpuInfo());
            existing.setMemoryTotal(dto.getMemoryTotal());
            existing.setStatus("online");
            existing.setLastHeartbeat(LocalDateTime.now());
            existing.setRegisteredAt(LocalDateTime.now());
            nodeMapper.updateById(existing);
            log.info("节点重新注册: id={}, name={}, address={}:{}", existing.getId(), dto.getNodeName(), dto.getHost(), dto.getPort());
            return NodeVO.fromEntity(existing);
        }

        // 新节点
        InferenceNode node = new InferenceNode();
        // 如果推理服务带了已持久化的 nodeId，沿用
        if (dto.getNodeId() != null && !dto.getNodeId().isBlank()) {
            node.setId(dto.getNodeId());
        } else {
            node.setId(IdUtil.uuid());
        }
        node.setNodeName(dto.getNodeName());
        node.setHost(dto.getHost());
        node.setPort(dto.getPort());
        node.setDeviceType(dto.getDeviceType());
        node.setGpuName(dto.getGpuName());
        node.setGpuCount(dto.getGpuCount() != null ? dto.getGpuCount() : 0);
        node.setCpuInfo(dto.getCpuInfo());
        node.setMemoryTotal(dto.getMemoryTotal());
        node.setStatus("online");
        node.setLastHeartbeat(LocalDateTime.now());
        node.setRegisteredAt(LocalDateTime.now());
        node.setDeleted(0);

        nodeMapper.insert(node);
        log.info("节点注册成功: id={}, name={}, address={}:{}", node.getId(), dto.getNodeName(), dto.getHost(), dto.getPort());
        return NodeVO.fromEntity(node);
    }

    /**
     * 心跳处理
     */
    public void heartbeat(NodeHeartbeatDTO dto) {
        InferenceNode node = nodeMapper.selectById(dto.getNodeId());
        if (node == null) {
            throw new BizException("节点不存在: " + dto.getNodeId());
        }

        // 更新心跳时间和状态
        node.setLastHeartbeat(LocalDateTime.now());
        node.setStatus("online");
        nodeMapper.updateById(node);

        // 缓存运行时信息
        NodeRuntimeInfo info = new NodeRuntimeInfo();
        info.setLoadedModels(dto.getLoadedModels());
        info.setActiveTasks(dto.getActiveTasks());
        info.setSystemLoad(dto.getSystemLoad());
        runtimeCache.put(dto.getNodeId(), info);
    }

    /**
     * 获取所有在线节点
     */
    public List<NodeVO> getOnlineNodes() {
        LambdaQueryWrapper<InferenceNode> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InferenceNode::getStatus, "online")
               .orderByDesc(InferenceNode::getRegisteredAt);

        return nodeMapper.selectList(wrapper).stream()
                .map(this::toVOWithRuntime)
                .collect(Collectors.toList());
    }

    /**
     * 获取所有节点
     */
    public List<NodeVO> getAllNodes() {
        LambdaQueryWrapper<InferenceNode> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(InferenceNode::getRegisteredAt);

        return nodeMapper.selectList(wrapper).stream()
                .map(this::toVOWithRuntime)
                .collect(Collectors.toList());
    }

    /**
     * 获取节点详情
     */
    public NodeVO getNodeById(String nodeId) {
        InferenceNode node = nodeMapper.selectById(nodeId);
        if (node == null) {
            throw new BizException("节点不存在");
        }
        return toVOWithRuntime(node);
    }

    /**
     * 修改节点名称
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateNodeName(String nodeId, String name) {
        InferenceNode node = nodeMapper.selectById(nodeId);
        if (node == null) {
            throw new BizException("节点不存在");
        }
        node.setNodeName(name);
        nodeMapper.updateById(node);
    }

    /**
     * 移除节点（软删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public void removeNode(String nodeId) {
        InferenceNode node = nodeMapper.selectById(nodeId);
        if (node == null) {
            throw new BizException("节点不存在");
        }
        nodeMapper.deleteById(nodeId);
        runtimeCache.invalidate(nodeId);
        log.info("节点已移除: id={}, name={}", nodeId, node.getNodeName());
    }

    /**
     * 标记节点离线
     */
    @Transactional(rollbackFor = Exception.class)
    public void markOffline(String nodeId) {
        InferenceNode node = nodeMapper.selectById(nodeId);
        if (node != null) {
            node.setStatus("offline");
            nodeMapper.updateById(node);
            runtimeCache.invalidate(nodeId);
            log.warn("节点标记为离线: id={}, name={}", nodeId, node.getNodeName());
        }
    }

    /**
     * 获取运行时缓存
     */
    public NodeRuntimeInfo getRuntimeInfo(String nodeId) {
        return runtimeCache.getIfPresent(nodeId);
    }

    private NodeVO toVOWithRuntime(InferenceNode entity) {
        NodeVO vo = NodeVO.fromEntity(entity);
        vo.setRuntimeInfo(runtimeCache.getIfPresent(entity.getId()));
        return vo;
    }
}
