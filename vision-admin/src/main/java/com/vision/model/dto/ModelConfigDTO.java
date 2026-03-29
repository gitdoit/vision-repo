package com.vision.model.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 模型配置更新DTO
 */
@Data
public class ModelConfigDTO {

    /**
     * 置信度阈值 (0-1)
     */
    @DecimalMin(value = "0.0", message = "置信度阈值不能小于0")
    @DecimalMax(value = "1.0", message = "置信度阈值不能大于1")
    private BigDecimal confidenceThreshold;

    /**
     * 输入分辨率: 640x640
     */
    private String inputResolution;

    /**
     * 最大并发数
     */
    @Min(value = 1, message = "最大并发数不能小于1")
    @Max(value = 10, message = "最大并发数不能大于10")
    private Integer maxConcurrency;
}
