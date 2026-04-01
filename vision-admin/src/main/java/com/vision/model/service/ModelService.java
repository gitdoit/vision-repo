package com.vision.model.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vision.model.dto.ModelConfigDTO;
import com.vision.model.dto.ModelCreateDTO;
import com.vision.model.dto.ModelDeploymentVO;
import com.vision.model.dto.ModelVO;
import com.vision.model.entity.Model;
import com.vision.model.entity.ModelNodeDeployment;
import com.vision.model.entity.ModelVersion;
import com.vision.model.mapper.ModelMapper;
import com.vision.model.mapper.ModelNodeDeploymentMapper;
import com.vision.model.mapper.ModelVersionMapper;
import com.vision.common.exception.BizException;
import com.vision.common.util.IdUtil;
import com.vision.node.entity.InferenceNode;
import com.vision.node.event.NodeOnlineEvent;
import com.vision.node.mapper.InferenceNodeMapper;
import com.vision.node.service.NodeRouter;
import com.vision.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 模型服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModelService extends ServiceImpl<ModelMapper, Model> {

    private final ModelMapper modelMapper;
    private final ModelVersionMapper modelVersionMapper;
    private final ModelNodeDeploymentMapper deploymentMapper;
    private final InferenceNodeMapper nodeMapper;
    private final InferenceClient inferenceClient;
    private final StorageService storageService;
    private final NodeRouter nodeRouter;

    /**
     * 分页查询模型列表（含部署信息）
     */
    public IPage<ModelVO> pageModels(Integer page, Integer size, String parsedStatus) {
        Page<Model> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Model> wrapper = new LambdaQueryWrapper<>();

        if (parsedStatus != null && !parsedStatus.isEmpty()) {
            wrapper.eq(Model::getParsedStatus, parsedStatus);
        }

        wrapper.orderByDesc(Model::getCreatedAt);

        IPage<Model> modelPage = modelMapper.selectPage(pageParam, wrapper);

        List<ModelVO> voList = modelPage.getRecords().stream()
                .map(this::toVOWithDeployments)
                .collect(Collectors.toList());

        Page<ModelVO> resultPage = new Page<>(modelPage.getCurrent(), modelPage.getSize(), modelPage.getTotal());
        resultPage.setRecords(voList);
        return resultPage;
    }

    /**
     * 根据ID查询模型详情（含部署信息）
     */
    public ModelVO getModelById(String id) {
        Model model = modelMapper.selectById(id);
        if (model == null) {
            throw new BizException("模型不存在");
        }
        return toVOWithDeployments(model);
    }

    /**
     * 创建模型
     */
    @Transactional(rollbackFor = Exception.class)
    public ModelVO createModel(ModelCreateDTO dto) {
        Model model = new Model();
        BeanUtils.copyProperties(dto, model);
        model.setId(IdUtil.uuid());
        model.setParsedStatus("pending");
        model.setConfidenceThreshold(BigDecimal.valueOf(0.25));
        model.setInputResolution("640x640");
        model.setMaxConcurrency(1);
        model.setAvgLatency(0);

        modelMapper.insert(model);

        // 创建版本记录
        createVersionRecord(model.getId(), dto.getVersion(), "初始版本");

        log.info("创建模型成功: id={}, name={}", model.getId(), model.getName());
        return toVOWithDeployments(model);
    }

    /**
     * 更新模型
     */
    @Transactional(rollbackFor = Exception.class)
    public ModelVO updateModel(String id, ModelCreateDTO dto) {
        Model model = modelMapper.selectById(id);
        if (model == null) {
            throw new BizException("模型不存在");
        }

        // 如果版本号变化，记录版本历史
        if (!dto.getVersion().equals(model.getVersion())) {
            createVersionRecord(id, dto.getVersion(), "版本更新");
        }

        BeanUtils.copyProperties(dto, model);
        modelMapper.updateById(model);

        log.info("更新模型成功: id={}", id);
        return toVOWithDeployments(model);
    }

    /**
     * 删除模型（先卸载所有节点上的部署）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteModel(String id) {
        Model model = modelMapper.selectById(id);
        if (model == null) {
            throw new BizException("模型不存在");
        }

        // 卸载所有节点上的部署
        List<ModelNodeDeployment> deployments = listDeployments(id);
        for (ModelNodeDeployment d : deployments) {
            if ("loaded".equals(d.getStatus())) {
                try {
                    inferenceClient.unloadModel(d.getNodeId(), id);
                } catch (Exception e) {
                    log.warn("卸载模型失败（删除时忽略）: modelId={}, nodeId={}", id, d.getNodeId(), e);
                }
            }
            deploymentMapper.deleteById(d.getId());
        }

        modelMapper.deleteById(id);
        log.info("删除模型成功: id={}", id);
    }

    /**
     * 加载模型到指定节点（支持多节点部署）
     */
    @Transactional(rollbackFor = Exception.class)
    public void loadModel(String id, String device, String deviceName, String nodeId) {
        Model model = modelMapper.selectById(id);
        if (model == null) {
            throw new BizException("模型不存在");
        }

        // 选择目标节点
        String targetNodeId = nodeRouter.selectNodeForLoad(nodeId);

        // 检查是否已在该节点部署
        LambdaQueryWrapper<ModelNodeDeployment> existWrapper = new LambdaQueryWrapper<>();
        existWrapper.eq(ModelNodeDeployment::getModelId, id)
                    .eq(ModelNodeDeployment::getNodeId, targetNodeId);
        ModelNodeDeployment existing = deploymentMapper.selectOne(existWrapper);
        if (existing != null && "loaded".equals(existing.getStatus())) {
            throw new BizException("模型已在该节点加载");
        }

        // 创建或更新部署记录
        ModelNodeDeployment deployment;
        if (existing != null) {
            deployment = existing;
        } else {
            deployment = new ModelNodeDeployment();
            deployment.setId(IdUtil.uuid());
            deployment.setModelId(id);
            deployment.setNodeId(targetNodeId);
        }
        deployment.setDevice(device);
        deployment.setDeviceName(deviceName);
        deployment.setStatus("loading");
        deployment.setDeployedAt(LocalDateTime.now());

        if (existing != null) {
            deploymentMapper.updateById(deployment);
        } else {
            deploymentMapper.insert(deployment);
        }

        log.info("开始加载模型: id={}, path={}, device={}, nodeId={}", id, model.getModelPath(), device, targetNodeId);

        try {
            // 读取模型文件并推送到推理节点
            String modelPath = model.getModelPath();
            byte[] fileBytes = storageService.readBytes(modelPath);
            String filename = storageService.extractFileName(modelPath);

            // 上传模型文件到推理节点
            String remoteModelPath = inferenceClient.uploadModelFile(targetNodeId, fileBytes, filename);

            // 调用推理节点加载模型
            inferenceClient.loadModel(targetNodeId, id, remoteModelPath, device);

            // 更新部署状态为 loaded
            deployment.setStatus("loaded");
            deploymentMapper.updateById(deployment);

            // 如果模型尚未解析，利用此次加载的节点进行解析
            if ("pending".equals(model.getParsedStatus())) {
                tryParseModelByPath(model, targetNodeId, remoteModelPath);
            }

            log.info("模型加载成功: id={}, nodeId={}", id, targetNodeId);
        } catch (BizException e) {
            deployment.setStatus("error");
            deploymentMapper.updateById(deployment);
            throw e;
        } catch (Exception e) {
            deployment.setStatus("error");
            deploymentMapper.updateById(deployment);
            log.error("模型加载失败: id={}", id, e);
            throw new BizException("模型加载失败: " + e.getMessage());
        }
    }

    /**
     * 从指定节点卸载模型
     */
    @Transactional(rollbackFor = Exception.class)
    public void unloadModel(String id, String nodeId) {
        Model model = modelMapper.selectById(id);
        if (model == null) {
            throw new BizException("模型不存在");
        }

        LambdaQueryWrapper<ModelNodeDeployment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ModelNodeDeployment::getModelId, id)
               .eq(ModelNodeDeployment::getNodeId, nodeId);
        ModelNodeDeployment deployment = deploymentMapper.selectOne(wrapper);
        if (deployment == null) {
            throw new BizException("模型未在该节点部署");
        }

        try {
            inferenceClient.unloadModel(nodeId, id);
        } catch (Exception e) {
            log.warn("推理服务卸载模型失败（可能已下线）: modelId={}, nodeId={}", id, nodeId, e);
        }

        deploymentMapper.deleteById(deployment.getId());
        log.info("模型卸载成功: id={}, nodeId={}", id, nodeId);
    }

    /**
     * 从所有节点卸载模型（兼容旧接口）
     */
    @Transactional(rollbackFor = Exception.class)
    public void unloadModelAll(String id) {
        List<ModelNodeDeployment> deployments = listDeployments(id);
        for (ModelNodeDeployment d : deployments) {
            try {
                unloadModel(id, d.getNodeId());
            } catch (Exception e) {
                log.warn("卸载模型失败: modelId={}, nodeId={}", id, d.getNodeId(), e);
            }
        }
    }

    /**
     * 更新模型配置
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateModelConfig(String id, ModelConfigDTO dto) {
        Model model = modelMapper.selectById(id);
        if (model == null) {
            throw new BizException("模型不存在");
        }

        if (dto.getConfidenceThreshold() != null) {
            model.setConfidenceThreshold(dto.getConfidenceThreshold());
        }
        if (dto.getInputResolution() != null) {
            model.setInputResolution(dto.getInputResolution());
        }
        if (dto.getMaxConcurrency() != null) {
            model.setMaxConcurrency(dto.getMaxConcurrency());
        }

        modelMapper.updateById(model);
        log.info("更新模型配置成功: id={}", id);
    }

    /**
     * 获取模型版本历史
     */
    public List<ModelVersion> getModelVersions(String modelId) {
        LambdaQueryWrapper<ModelVersion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ModelVersion::getModelId, modelId);
        wrapper.orderByDesc(ModelVersion::getCreatedAt);
        return modelVersionMapper.selectList(wrapper);
    }

    /**
     * 上传模型文件并创建记录。
     * 尝试调用在线推理节点解析模型元数据（类别信息等），
     * 无在线节点则标记为 pending，后续节点上线时自动补全。
     */
    @Transactional(rollbackFor = Exception.class)
    public ModelVO uploadModel(MultipartFile file, ModelCreateDTO dto) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new BizException("文件名不能为空");
        }

        // 存储模型文件：models/{date}/{uuid}_{原始文件名}
        String date = LocalDateTime.now().toLocalDate().toString();
        String storagePath = "models/" + date + "/" + IdUtil.uuid() + "_" + originalFilename;

        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
            String url = storageService.upload(file.getInputStream(), storagePath, file.getContentType());
            dto.setModelPath(url);
        } catch (Exception e) {
            log.error("模型文件上传失败: {}", originalFilename, e);
            throw new BizException("模型文件上传失败: " + e.getMessage());
        }

        ModelVO vo = createModel(dto);

        // 尝试解析模型元数据
        tryParseModelOnUpload(vo.getId(), fileBytes, originalFilename);

        // 重新查询以包含解析结果
        return getModelById(vo.getId());
    }

    /**
     * 查询模型在哪些节点上已部署
     */
    public List<ModelNodeDeployment> listDeployments(String modelId) {
        LambdaQueryWrapper<ModelNodeDeployment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ModelNodeDeployment::getModelId, modelId);
        return deploymentMapper.selectList(wrapper);
    }

    /**
     * 查询模型的一个 loaded 状态的部署（用于推理路由）
     */
    public ModelNodeDeployment findLoadedDeployment(String modelId) {
        LambdaQueryWrapper<ModelNodeDeployment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ModelNodeDeployment::getModelId, modelId)
               .eq(ModelNodeDeployment::getStatus, "loaded")
               .last("LIMIT 1");
        return deploymentMapper.selectOne(wrapper);
    }

    /**
     * 查询所有 pending 解析的模型
     */
    public List<Model> listPendingModels() {
        LambdaQueryWrapper<Model> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Model::getParsedStatus, "pending");
        return modelMapper.selectList(wrapper);
    }

    /**
     * 解析模型元数据并更新到数据库
     */
    @SuppressWarnings("unchecked")
    public void applyParseResult(String modelId, Map<String, Object> parseResult) {
        Model model = modelMapper.selectById(modelId);
        if (model == null) return;

        List<String> classNames = (List<String>) parseResult.get("class_names");
        Integer numClasses = parseResult.get("num_classes") != null
                ? ((Number) parseResult.get("num_classes")).intValue() : null;
        String taskType = (String) parseResult.get("task_type");
        Integer inputSize = parseResult.get("input_size") != null
                ? ((Number) parseResult.get("input_size")).intValue() : null;

        if (classNames != null && !classNames.isEmpty()) {
            model.setClassNames(String.join(",", classNames));
            model.setNumClasses(classNames.size());
        } else if (numClasses != null) {
            model.setNumClasses(numClasses);
        }

        // 自动覆盖用户手填的 taskType 和 inputResolution
        if (taskType != null && !taskType.isEmpty()) {
            model.setTaskType(taskType);
        }
        if (inputSize != null && inputSize > 0) {
            model.setInputResolution(inputSize + "x" + inputSize);
        }

        model.setParsedStatus("parsed");
        modelMapper.updateById(model);
        log.info("模型元数据解析成功: id={}, classes={}, taskType={}", modelId, model.getClassNames(), model.getTaskType());
    }

    // ---- 内部方法 ----

    /**
     * 上传时尝试解析：找一个在线节点发送文件解析
     */
    private void tryParseModelOnUpload(String modelId, byte[] fileBytes, String filename) {
        try {
            String onlineNodeId = nodeRouter.selectAnyOnlineNode();
            if (onlineNodeId == null) {
                log.info("无在线推理节点，模型解析推迟: modelId={}", modelId);
                return;
            }
            Map<String, Object> result = inferenceClient.parseModel(onlineNodeId, fileBytes, filename);
            applyParseResult(modelId, result);
        } catch (Exception e) {
            log.warn("模型上传时解析失败（标记为pending，后续重试）: modelId={}", modelId, e);
        }
    }

    /**
     * 加载时通过已上传路径解析（无需重复传文件）
     */
    private void tryParseModelByPath(Model model, String nodeId, String remoteModelPath) {
        try {
            Map<String, Object> result = inferenceClient.parseModelByPath(nodeId, remoteModelPath);
            applyParseResult(model.getId(), result);
        } catch (Exception e) {
            log.warn("模型加载时解析失败: modelId={}", model.getId(), e);
        }
    }

    private ModelVO toVOWithDeployments(Model entity) {
        ModelVO vo = ModelVO.fromEntity(entity);
        List<ModelNodeDeployment> deployments = listDeployments(entity.getId());
        List<ModelDeploymentVO> deploymentVOs = deployments.stream()
                .map(d -> {
                    ModelDeploymentVO dvo = ModelDeploymentVO.fromEntity(d);
                    // 填充 nodeName
                    InferenceNode node = nodeMapper.selectById(d.getNodeId());
                    if (node != null) {
                        dvo.setNodeName(node.getNodeName());
                    }
                    return dvo;
                })
                .collect(Collectors.toList());
        vo.setDeployments(deploymentVOs);
        return vo;
    }

    private void createVersionRecord(String modelId, String version, String description) {
        ModelVersion modelVersion = new ModelVersion();
        modelVersion.setId(IdUtil.uuid());
        modelVersion.setModelId(modelId);
        modelVersion.setVersion(version);
        modelVersion.setDescription(description);
        modelVersion.setCreatedAt(LocalDateTime.now());
        modelVersionMapper.insert(modelVersion);
    }

    /**
     * 节点上线时：
     * 1. 自动解析所有待解析的模型
     * 2. 同步该节点上已加载的模型到 model_node_deployment 表
     */
    @Async
    @EventListener
    public void onNodeOnline(NodeOnlineEvent event) {
        String nodeId = event.getNodeId();

        // 1. 自动解析待解析模型
        List<Model> pendingModels = listPendingModels();
        if (!pendingModels.isEmpty()) {
            log.info("节点上线，开始自动解析待解析模型: nodeId={}, count={}", nodeId, pendingModels.size());
            for (Model model : pendingModels) {
                try {
                    byte[] fileBytes = storageService.readBytes(model.getModelPath());
                    String filename = storageService.extractFileName(model.getModelPath());
                    Map<String, Object> result = inferenceClient.parseModel(nodeId, fileBytes, filename);
                    applyParseResult(model.getId(), result);
                    log.info("自动解析完成: modelId={}, modelName={}", model.getId(), model.getName());
                } catch (Exception e) {
                    log.warn("自动解析失败: modelId={}, modelName={}", model.getId(), model.getName(), e);
                }
            }
        }

        // 2. 同步节点上已加载的模型到部署表
        syncNodeDeployments(nodeId);
    }

    /**
     * 同步推理节点上实际加载的模型与 Java 侧部署记录。
     * - 节点上有、Java 侧没有 → 创建部署记录
     * - Java 侧有、节点上没有 → 删除过期部署记录
     */
    private void syncNodeDeployments(String nodeId) {
        try {
            Map<String, Map<String, Object>> remoteModels = inferenceClient.getModelsStatus(nodeId);
            log.info("同步节点模型部署: nodeId={}, 节点上已加载模型数={}", nodeId, remoteModels.size());

            // 查询 Java 侧该节点的所有现有部署记录
            LambdaQueryWrapper<ModelNodeDeployment> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ModelNodeDeployment::getNodeId, nodeId);
            List<ModelNodeDeployment> existingDeployments = deploymentMapper.selectList(wrapper);
            Set<String> existingModelIds = existingDeployments.stream()
                    .map(ModelNodeDeployment::getModelId)
                    .collect(Collectors.toSet());

            // 节点上有但 Java 侧没有 → 创建部署记录
            for (Map.Entry<String, Map<String, Object>> entry : remoteModels.entrySet()) {
                String modelId = entry.getKey();
                Map<String, Object> info = entry.getValue();

                // 校验模型在 Java 侧存在
                Model model = modelMapper.selectById(modelId);
                if (model == null) {
                    log.debug("节点上的模型在数据库中不存在，跳过同步: modelId={}", modelId);
                    continue;
                }

                if (!existingModelIds.contains(modelId)) {
                    ModelNodeDeployment deployment = new ModelNodeDeployment();
                    deployment.setId(IdUtil.uuid());
                    deployment.setModelId(modelId);
                    deployment.setNodeId(nodeId);
                    deployment.setDevice(info.getOrDefault("device", "cpu").toString());
                    deployment.setStatus("loaded");
                    deployment.setDeployedAt(LocalDateTime.now());
                    deploymentMapper.insert(deployment);
                    log.info("同步补录部署记录: modelId={}, nodeId={}", modelId, nodeId);
                }
            }

            // Java 侧有但节点上没有 → 删除过期部署记录
            Set<String> remoteModelIds = remoteModels.keySet();
            for (ModelNodeDeployment d : existingDeployments) {
                if (!remoteModelIds.contains(d.getModelId())) {
                    deploymentMapper.deleteById(d.getId());
                    log.info("清理过期部署记录: modelId={}, nodeId={}", d.getModelId(), nodeId);
                }
            }
        } catch (Exception e) {
            log.warn("同步节点模型部署失败: nodeId={}", nodeId, e);
        }
    }
}
