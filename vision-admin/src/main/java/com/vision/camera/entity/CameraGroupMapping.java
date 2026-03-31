package com.vision.camera.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 摄像头-分组关联实体
 * 对应表: camera_group_mapping
 */
@Data
@TableName("camera_group_mapping")
public class CameraGroupMapping {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private String cameraId;

    private String groupId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
