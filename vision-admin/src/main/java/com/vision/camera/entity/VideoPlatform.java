package com.vision.camera.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 视频平台实体
 * 对应表: video_platform
 */
@Data
@TableName("video_platform")
public class VideoPlatform {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private String name;

    private String apiBase;

    private String authType;

    private String credential;

    private Boolean autoSync;

    private Integer syncIntervalMin;

    private LocalDateTime lastSyncTime;

    private String lastSyncResult;

    private Integer camerasCount;

    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
