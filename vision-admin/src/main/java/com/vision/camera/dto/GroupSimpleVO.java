package com.vision.camera.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分组简要信息（用于摄像头VO中展示所属分组）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroupSimpleVO {

    private String id;

    private String name;
}
