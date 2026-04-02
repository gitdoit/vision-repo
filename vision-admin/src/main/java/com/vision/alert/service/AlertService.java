package com.vision.alert.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vision.alert.dto.AlertQueryDTO;
import com.vision.alert.dto.AlertVO;
import com.vision.alert.entity.Alert;
import com.vision.alert.mapper.AlertMapper;
import com.vision.camera.entity.Camera;
import com.vision.camera.mapper.CameraMapper;
import com.vision.common.util.IdUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 告警服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService extends ServiceImpl<AlertMapper, Alert> {

    private final AlertMapper alertMapper;
    private final AlertPushService alertPushService;
    private final CameraMapper cameraMapper;

    /**
     * 分页查询告警列表
     */
    public IPage<AlertVO> pageAlerts(AlertQueryDTO dto) {
        Page<Alert> pageParam = new Page<>(dto.getPage(), dto.getSize());
        LambdaQueryWrapper<Alert> wrapper = new LambdaQueryWrapper<>();

        if (dto.getAlertLevel() != null && !dto.getAlertLevel().isEmpty()) {
            wrapper.eq(Alert::getAlertLevel, dto.getAlertLevel());
        }
        if (dto.getAlertType() != null && !dto.getAlertType().isEmpty()) {
            wrapper.eq(Alert::getAlertType, dto.getAlertType());
        }
        if (dto.getCameraId() != null && !dto.getCameraId().isEmpty()) {
            wrapper.eq(Alert::getCameraId, dto.getCameraId());
        }
        if (dto.getReadStatus() != null) {
            wrapper.eq(Alert::getReadStatus, dto.getReadStatus());
        }
        if (dto.getStartTime() != null) {
            wrapper.ge(Alert::getAlertTime, dto.getStartTime());
        }
        if (dto.getEndTime() != null) {
            wrapper.le(Alert::getAlertTime, dto.getEndTime());
        }

        wrapper.orderByDesc(Alert::getAlertTime);

        IPage<Alert> alertPage = alertMapper.selectPage(pageParam, wrapper);

        List<AlertVO> voList = alertPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        Page<AlertVO> resultPage = new Page<>(alertPage.getCurrent(), alertPage.getSize(), alertPage.getTotal());
        resultPage.setRecords(voList);
        return resultPage;
    }

    /**
     * 根据ID查询告警详情
     */
    public AlertVO getAlertById(String id) {
        Alert alert = alertMapper.selectById(id);
        if (alert == null) {
            return null;
        }
        return convertToVO(alert);
    }

    /**
     * 创建告警
     */
    @Transactional(rollbackFor = Exception.class)
    public AlertVO createAlert(AlertVO vo) {
        Alert alert = new Alert();
        alert.setId(IdUtil.uuid());
        alert.setAlertLevel(vo.getAlertLevel());
        alert.setAlertType(vo.getAlertType());
        alert.setScene(vo.getScene());
        alert.setCameraId(vo.getCameraId());
        alert.setStreamId(vo.getStreamId());
        alert.setCaptureTime(vo.getCaptureTime());
        alert.setAlertTime(LocalDateTime.now());
        alert.setTriggerCondition(vo.getTriggerCondition());
        alert.setRelatedObjects(vo.getRelatedObjects());
        alert.setEvidence(vo.getEvidence());
        alert.setLocation(vo.getLocation());
        alert.setRuleId(vo.getRuleId());
        alert.setReadStatus(false);
        alert.setCreatedAt(LocalDateTime.now());

        alertMapper.insert(alert);

        // 实时推送
        alertPushService.pushAlert(convertToVO(alert));

        log.info("创建告警成功: id={}, level={}, type={}", alert.getId(), alert.getAlertLevel(), alert.getAlertType());
        return convertToVO(alert);
    }

    /**
     * 标记已读
     */
    @Transactional(rollbackFor = Exception.class)
    public void markAsRead(String id) {
        Alert alert = alertMapper.selectById(id);
        if (alert != null) {
            alert.setReadStatus(true);
            alertMapper.updateById(alert);
            log.debug("告警标记已读: id={}", id);
        }
    }

    /**
     * 批量标记已读
     */
    @Transactional(rollbackFor = Exception.class)
    public void markBatchAsRead(List<String> ids) {
        for (String id : ids) {
            markAsRead(id);
        }
    }

    /**
     * 全部标记已读
     */
    @Transactional(rollbackFor = Exception.class)
    public void markAllAsRead() {
        LambdaQueryWrapper<Alert> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Alert::getReadStatus, false);

        List<Alert> alerts = alertMapper.selectList(wrapper);
        for (Alert alert : alerts) {
            alert.setReadStatus(true);
            alertMapper.updateById(alert);
        }
        log.info("全部标记已读: count={}", alerts.size());
    }

    /**
     * 获取未读告警数量
     */
    public Long getUnreadCount() {
        LambdaQueryWrapper<Alert> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Alert::getReadStatus, false);
        return alertMapper.selectCount(wrapper);
    }

    /**
     * 获取最新告警列表
     */
    public List<AlertVO> getLatestAlerts(Integer limit) {
        LambdaQueryWrapper<Alert> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Alert::getAlertTime);
        wrapper.last("LIMIT " + (limit != null ? limit : 10));

        return alertMapper.selectList(wrapper).stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    /**
     * 转换为VO
     */
    private AlertVO convertToVO(Alert alert) {
        AlertVO vo = AlertVO.fromEntity(alert);
        if (alert.getCameraId() != null) {
            Camera camera = cameraMapper.selectById(alert.getCameraId());
            if (camera != null) {
                vo.setCameraName(camera.getName());
            }
        }
        return vo;
    }
}
