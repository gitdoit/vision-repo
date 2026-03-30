package com.vision.camera.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 视频平台批量导入请求DTO
 */
@Data
public class PlatformImportDTO {

    /** 平台 API 基础地址，如 http://10.100.121.12:18080 */
    @NotBlank(message = "平台地址不能为空")
    private String apiBase;

    /** 登录用户名 */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /** 登录密码（MD5） */
    @NotBlank(message = "密码不能为空")
    private String password;
}
