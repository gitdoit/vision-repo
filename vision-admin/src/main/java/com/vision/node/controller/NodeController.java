package com.vision.node.controller;

import com.vision.common.response.R;
import com.vision.model.service.InferenceClient;
import com.vision.node.dto.NodeHeartbeatDTO;
import com.vision.node.dto.NodeRegisterDTO;
import com.vision.node.dto.NodeVO;
import com.vision.node.service.NodeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 推理节点管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/nodes")
@RequiredArgsConstructor
public class NodeController {

    private final NodeService nodeService;
    private final InferenceClient inferenceClient;

    /**
     * 节点注册（推理服务调用）
     */
    @PostMapping("/register")
    public R<NodeVO> register(@Valid @RequestBody NodeRegisterDTO dto) {
        log.info("节点注册请求: name={}, address={}:{}", dto.getNodeName(), dto.getHost(), dto.getPort());
        NodeVO vo = nodeService.register(dto);
        return R.ok(vo);
    }

    /**
     * 节点心跳（推理服务调用）
     */
    @PostMapping("/heartbeat")
    public R<Void> heartbeat(@Valid @RequestBody NodeHeartbeatDTO dto) {
        nodeService.heartbeat(dto);
        return R.ok();
    }

    /**
     * 获取所有节点列表
     */
    @GetMapping
    public R<List<NodeVO>> listNodes() {
        return R.ok(nodeService.getAllNodes());
    }

    /**
     * 获取节点详情
     */
    @GetMapping("/{id}")
    public R<NodeVO> getNode(@PathVariable String id) {
        return R.ok(nodeService.getNodeById(id));
    }

    /**
     * 修改节点名称
     */
    @PutMapping("/{id}/name")
    public R<Void> updateNodeName(@PathVariable String id, @RequestBody Map<String, String> body) {
        String name = body.get("name");
        nodeService.updateNodeName(id, name);
        return R.ok();
    }

    /**
     * 移除节点
     */
    @DeleteMapping("/{id}")
    public R<Void> removeNode(@PathVariable String id) {
        log.info("移除节点: id={}", id);
        nodeService.removeNode(id);
        return R.ok();
    }

    /**
     * 重载推理节点（使 Python 代码变更生效）
     */
    @PostMapping("/{id}/reload")
    public R<Map<String, Object>> reloadNode(@PathVariable String id) {
        log.info("重载推理节点: id={}", id);
        Map<String, Object> result = inferenceClient.reloadNode(id);
        return R.ok(result);
    }
}
