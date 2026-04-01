package com.vision.task.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vision.camera.entity.CameraGroup;
import com.vision.camera.entity.CameraGroupMapping;
import com.vision.camera.mapper.CameraGroupMapper;
import com.vision.camera.mapper.CameraGroupMappingMapper;
import com.vision.common.exception.BizException;
import com.vision.model.entity.Model;
import com.vision.model.mapper.ModelMapper;
import com.vision.model.service.ModelService;
import com.vision.task.dto.MonitorTaskCreateDTO;
import com.vision.task.dto.MonitorTaskVO;
import com.vision.task.entity.MonitorTask;
import com.vision.task.mapper.MonitorTaskMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 监测任务服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MonitorTaskService extends ServiceImpl<MonitorTaskMapper, MonitorTask> {

    private final MonitorTaskMapper monitorTaskMapper;
    private final CameraGroupMapper cameraGroupMapper;
    private final CameraGroupMappingMapper cameraGroupMappingMapper;
    private final ModelMapper modelMapper;
    private final ModelService modelService;

    /**
     * 分页查询监测任务
     */
    public IPage<MonitorTaskVO> pageTasks(Integer page, Integer size, String status, String businessLine, String modelId) {
        Page<MonitorTask> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<MonitorTask> wrapper = new LambdaQueryWrapper<>();

        if (status != null && !status.isEmpty()) {
            wrapper.eq(MonitorTask::getStatus, status);
        }
        if (businessLine != null && !businessLine.isEmpty()) {
            wrapper.eq(MonitorTask::getBusinessLine, businessLine);
        }
        if (modelId != null && !modelId.isEmpty()) {
            wrapper.eq(MonitorTask::getModelId, modelId);
        }

        wrapper.orderByDesc(MonitorTask::getCreatedAt);

        IPage<MonitorTask> taskPage = monitorTaskMapper.selectPage(pageParam, wrapper);

        List<MonitorTaskVO> voList = taskPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        Page<MonitorTaskVO> resultPage = new Page<>(taskPage.getCurrent(), taskPage.getSize(), taskPage.getTotal());
        resultPage.setRecords(voList);
        return resultPage;
    }

    /**
     * 根据ID查询任务详情
     */
    public MonitorTaskVO getTaskById(String id) {
        MonitorTask task = monitorTaskMapper.selectById(id);
        if (task == null) {
            throw new BizException("监测任务不存在");
        }
        return convertToVO(task);
    }

    /**
     * 创建监测任务
     */
    @Transactional(rollbackFor = Exception.class)
    public MonitorTaskVO createTask(MonitorTaskCreateDTO dto) {
        // 校验分组存在
        CameraGroup group = cameraGroupMapper.selectById(dto.getGroupId());
        if (group == null) {
            throw new BizException("摄像头分组不存在");
        }

        // 校验模型存在
        Model model = modelMapper.selectById(dto.getModelId());
        if (model == null) {
            throw new BizException("模型不存在");
        }

        MonitorTask task = new MonitorTask();
        BeanUtils.copyProperties(dto, task);
        task.setStatus("stopped");
        task.setTotalInference(0L);
        task.setTotalAlert(0L);

        // 默认值
        if (task.getCaptureFrequency() == null || task.getCaptureFrequency().isBlank()) {
            task.setCaptureFrequency("5min");
        }
        if (task.getAlertConfidence() == null) {
            task.setAlertConfidence(new java.math.BigDecimal("0.50"));
        }
        if (task.getAlertFrames() == null) {
            task.setAlertFrames(1);
        }
        if (task.getAlertLevel() == null || task.getAlertLevel().isBlank()) {
            task.setAlertLevel("warning");
        }

        monitorTaskMapper.insert(task);

        log.info("创建监测任务: id={}, name={}", task.getId(), task.getName());
        return convertToVO(task);
    }

    /**
     * 更新监测任务
     */
    @Transactional(rollbackFor = Exception.class)
    public MonitorTaskVO updateTask(String id, MonitorTaskCreateDTO dto) {
        MonitorTask task = monitorTaskMapper.selectById(id);
        if (task == null) {
            throw new BizException("监测任务不存在");
        }

        if ("running".equals(task.getStatus())) {
            throw new BizException("运行中的任务不能编辑，请先停止");
        }

        // 校验分组存在
        if (dto.getGroupId() != null) {
            CameraGroup group = cameraGroupMapper.selectById(dto.getGroupId());
            if (group == null) {
                throw new BizException("摄像头分组不存在");
            }
        }

        // 校验模型存在
        if (dto.getModelId() != null) {
            Model model = modelMapper.selectById(dto.getModelId());
            if (model == null) {
                throw new BizException("模型不存在");
            }
        }

        BeanUtils.copyProperties(dto, task, "id", "status", "totalInference", "totalAlert",
                "lastInferenceTime", "lastAlertTime", "createdAt", "deleted");

        monitorTaskMapper.updateById(task);

        log.info("更新监测任务: id={}", id);
        return convertToVO(task);
    }

    /**
     * 删除监测任务
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteTask(String id) {
        MonitorTask task = monitorTaskMapper.selectById(id);
        if (task == null) {
            throw new BizException("监测任务不存在");
        }
        if ("running".equals(task.getStatus())) {
            throw new BizException("运行中的任务不能删除，请先停止");
        }

        monitorTaskMapper.deleteById(id);
        log.info("删除监测任务: id={}", id);
    }

    /**
     * 启动监测任务
     */
    @Transactional(rollbackFor = Exception.class)
    public void startTask(String id) {
        MonitorTask task = monitorTaskMapper.selectById(id);
        if (task == null) {
            throw new BizException("监测任务不存在");
        }
        if ("running".equals(task.getStatus())) {
            throw new BizException("任务已在运行中");
        }

        // 校验模型已加载（至少一个节点上加载了该模型）
        Model model = modelMapper.selectById(task.getModelId());
        if (model == null) {
            throw new BizException("关联模型不存在");
        }
        // 检查模型至少在一个节点上加载
        com.vision.model.entity.ModelNodeDeployment deployment =
                modelService.findLoadedDeployment(model.getId());
        if (deployment == null) {
            throw new BizException("关联模型未加载，请先加载模型");
        }

        // 校验分组下有摄像头
        long cameraCount = cameraGroupMappingMapper.selectCount(
                new LambdaQueryWrapper<CameraGroupMapping>()
                        .eq(CameraGroupMapping::getGroupId, task.getGroupId()));
        if (cameraCount == 0) {
            throw new BizException("关联分组下没有摄像头");
        }

        task.setStatus("running");
        monitorTaskMapper.updateById(task);

        log.info("启动监测任务: id={}, name={}", id, task.getName());
    }

    /**
     * 停止监测任务
     */
    @Transactional(rollbackFor = Exception.class)
    public void stopTask(String id) {
        MonitorTask task = monitorTaskMapper.selectById(id);
        if (task == null) {
            throw new BizException("监测任务不存在");
        }
        if (!"running".equals(task.getStatus()) && !"error".equals(task.getStatus())) {
            throw new BizException("任务未在运行中");
        }

        task.setStatus("stopped");
        monitorTaskMapper.updateById(task);

        log.info("停止监测任务: id={}, name={}", id, task.getName());
    }

    /**
     * 查询所有运行中的任务
     */
    public List<MonitorTask> getRunningTasks() {
        return monitorTaskMapper.selectList(
                new LambdaQueryWrapper<MonitorTask>()
                        .eq(MonitorTask::getStatus, "running"));
    }

    /**
     * 实体转VO
     */
    private MonitorTaskVO convertToVO(MonitorTask task) {
        MonitorTaskVO vo = new MonitorTaskVO();
        BeanUtils.copyProperties(task, vo);

        // 填充分组名称
        if (task.getGroupId() != null) {
            CameraGroup group = cameraGroupMapper.selectById(task.getGroupId());
            if (group != null) {
                vo.setGroupName(group.getName());
            }
        }

        // 填充模型名称
        if (task.getModelId() != null) {
            Model model = modelMapper.selectById(task.getModelId());
            if (model != null) {
                vo.setModelName(model.getName());
            }
        }

        return vo;
    }
}
