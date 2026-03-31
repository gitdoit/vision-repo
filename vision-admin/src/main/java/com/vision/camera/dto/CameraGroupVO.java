package com.vision.camera.dto;

import com.vision.camera.entity.CameraGroup;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 摄像头分组树节点视图对象
 */
@Data
public class CameraGroupVO {

    /**
     * 分组ID
     */
    private String id;

    /**
     * 分组名称
     */
    private String name;

    /**
     * 父分组ID
     */
    private String parentId;

    /**
     * 图标
     */
    private String icon;

    /**
     * 排序序号
     */
    private Integer sortOrder;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 子分组列表
     */
    private List<CameraGroupVO> children = new ArrayList<>();

    /**
     * 关联摄像头数量
     */
    private Integer cameraCount = 0;

    /**
     * 从实体转换
     */
    public static CameraGroupVO fromEntity(CameraGroup entity) {
        CameraGroupVO vo = new CameraGroupVO();
        vo.setId(entity.getId());
        vo.setName(entity.getName());
        vo.setParentId(entity.getParentId());
        vo.setIcon(entity.getIcon());
        vo.setSortOrder(entity.getSortOrder());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }
}
