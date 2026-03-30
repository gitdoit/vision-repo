package com.vision.model.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vision.model.dto.ModelConfigDTO;
import com.vision.model.dto.ModelCreateDTO;
import com.vision.model.dto.ModelVO;
import com.vision.model.entity.Model;
import com.vision.model.entity.ModelVersion;
import com.vision.model.mapper.ModelMapper;
import com.vision.model.mapper.ModelVersionMapper;
import com.vision.common.exception.BizException;
import com.vision.common.util.IdUtil;
import com.vision.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
    private final InferenceClient inferenceClient;
    private final StorageService storageService;

    /**
     * 分页查询模型列表
     */
    public IPage<ModelVO> pageModels(Integer page, Integer size, String status) {
        Page<Model> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Model> wrapper = new LambdaQueryWrapper<>();

        if (status != null && !status.isEmpty()) {
            wrapper.eq(Model::getStatus, status);
        }

        wrapper.orderByDesc(Model::getCreatedAt);

        IPage<Model> modelPage = modelMapper.selectPage(pageParam, wrapper);

        List<ModelVO> voList = modelPage.getRecords().stream()
                .map(ModelVO::fromEntity)
                .collect(Collectors.toList());

        Page<ModelVO> resultPage = new Page<>(modelPage.getCurrent(), modelPage.getSize(), modelPage.getTotal());
        resultPage.setRecords(voList);
        return resultPage;
    }

    /**
     * 根据ID查询模型详情
     */
    public ModelVO getModelById(String id) {
        Model model = modelMapper.selectById(id);
        if (model == null) {
            throw new BizException("模型不存在");
        }
        return ModelVO.fromEntity(model);
    }

    /**
     * 创建模型
     */
    @Transactional(rollbackFor = Exception.class)
    public ModelVO createModel(ModelCreateDTO dto) {
        Model model = new Model();
        BeanUtils.copyProperties(dto, model);
        model.setId(IdUtil.uuid());
        model.setStatus("unloaded");
        model.setConfidenceThreshold(BigDecimal.valueOf(0.50));
        model.setInputResolution("640x640");
        model.setMaxConcurrency(1);
        model.setAvgLatency(0);

        modelMapper.insert(model);

        // 创建版本记录
        createVersionRecord(model.getId(), dto.getVersion(), "初始版本");

        log.info("创建模型成功: id={}, name={}", model.getId(), model.getName());
        return ModelVO.fromEntity(model);
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
        return ModelVO.fromEntity(model);
    }

    /**
     * 删除模型
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteModel(String id) {
        Model model = modelMapper.selectById(id);
        if (model == null) {
            throw new BizException("模型不存在");
        }

        // 如果模型已加载，先卸载
        if ("loaded".equals(model.getStatus())) {
            unloadModel(id);
            // 重新查询，unloadModel 已更新状态
            model = modelMapper.selectById(id);
        }

        modelMapper.deleteById(id);
        log.info("删除模型成功: id={}", id);
    }

    /**
     * 加载模型
     */
    @Transactional(rollbackFor = Exception.class)
    public void loadModel(String id, String device, String deviceName) {
        Model model = modelMapper.selectById(id);
        if (model == null) {
            throw new BizException("模型不存在");
        }

        if ("loaded".equals(model.getStatus())) {
            throw new BizException("模型已加载");
        }

        // 更新状态为加载中
        model.setStatus("loading");
        modelMapper.updateById(model);
        log.info("开始加载模型: id={}, path={}, device={}", id, model.getModelPath(), device);

        try {
            // 将存储 URL 转换为本地文件系统路径后发送给推理服务
            String localPath = storageService.resolveToFilePath(model.getModelPath());
            inferenceClient.loadModel(id, localPath, device);

            // 更新状态为已加载
            model.setStatus("loaded");
            model.setDevice(device);
            model.setDeviceName(deviceName);
            modelMapper.updateById(model);
            log.info("模型加载成功: id={}", id);
        } catch (Exception e) {
            model.setStatus("unloaded");
            modelMapper.updateById(model);
            log.error("模型加载失败: id={}", id, e);
            throw new BizException("模型加载失败: " + e.getMessage());
        }
    }

    /**
     * 卸载模型
     */
    @Transactional(rollbackFor = Exception.class)
    public void unloadModel(String id) {
        Model model = modelMapper.selectById(id);
        if (model == null) {
            throw new BizException("模型不存在");
        }

        if (!"loaded".equals(model.getStatus())) {
            throw new BizException("模型未加载");
        }

        try {
            // 调用Python推理服务卸载模型
            inferenceClient.unloadModel(id);

            // 更新状态
            model.setStatus("unloaded");
            model.setDevice(null);
            model.setDeviceName(null);
            modelMapper.updateById(model);
            log.info("模型卸载成功: id={}", id);
        } catch (Exception e) {
            log.error("模型卸载失败: id={}", id, e);
            throw new BizException("模型卸载失败: " + e.getMessage());
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
     * 上传模型文件并创建记录
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

        try {
            String url = storageService.upload(file.getInputStream(), storagePath, file.getContentType());
            dto.setModelPath(url);
        } catch (Exception e) {
            log.error("模型文件上传失败: {}", originalFilename, e);
            throw new BizException("模型文件上传失败: " + e.getMessage());
        }

        return createModel(dto);
    }

    /**
     * 创建版本记录
     */
    private void createVersionRecord(String modelId, String version, String description) {
        ModelVersion modelVersion = new ModelVersion();
        modelVersion.setId(IdUtil.uuid());
        modelVersion.setModelId(modelId);
        modelVersion.setVersion(version);
        modelVersion.setDescription(description);
        modelVersion.setCreatedAt(LocalDateTime.now());
        modelVersionMapper.insert(modelVersion);
    }
}
