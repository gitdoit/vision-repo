package com.vision.camera.dto;

import com.vision.camera.entity.Camera;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 摄像头视图对象
 */
@Data
public class CameraVO {

    /**
     * 主键ID
     */
    private String id;

    /**
     * 摄像头名称
     */
    private String name;

    /**
     * 业务线
     */
    private String businessLine;

    /**
     * 安装位置
     */
    private String location;

    /**
     * 视频流地址 (RTSP)
     */
    private String streamUrl;

    /**
     * 抓图频率
     */
    private String captureFrequency;

    /**
     * 是否启用AI分析
     */
    private Boolean aiEnabled;

    /**
     * 状态: online/offline/error
     */
    private String status;

    /**
     * 最后抓图时间
     */
    private LocalDateTime lastCaptureTime;

    /**
     * 所属分组列表
     */
    private List<GroupSimpleVO> groups = new ArrayList<>();

    /**
     * 来源: manual/synced
     */
    private String source;

    /**
     * 视频平台ID
     */
    private String platformId;

    /**
     * 通道号
     */
    private String channelNo;

    /**
     * 标签名称
     */
    private String label;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 从实体转换
     */
    public static CameraVO fromEntity(Camera entity) {
        CameraVO vo = new CameraVO();
        vo.setId(entity.getId());
        vo.setName(entity.getName());
        vo.setBusinessLine(entity.getBusinessLine());
        vo.setLocation(entity.getLocation());
        vo.setStreamUrl(entity.getStreamUrl());
        vo.setCaptureFrequency(entity.getCaptureFrequency());
        vo.setAiEnabled(entity.getAiEnabled());
        vo.setStatus(entity.getStatus());
        vo.setLastCaptureTime(entity.getLastCaptureTime());
        vo.setSource(entity.getSource());
        vo.setPlatformId(entity.getPlatformId());
        vo.setChannelNo(entity.getChannelNo());
        vo.setLabel(entity.getLabel());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }
}
