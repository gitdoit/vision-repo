package com.vision.inference.dto;

import com.vision.inference.entity.Detection;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 检测目标视图对象
 */
@Data
public class DetectionVO {

    private String id;
    private String recordId;
    private String label;
    private BigDecimal confidence;
    private String bbox;
    private Integer count;
    private String attributes;

    /**
     * 从实体转换
     */
    public static DetectionVO fromEntity(Detection entity) {
        DetectionVO vo = new DetectionVO();
        vo.setId(entity.getId());
        vo.setRecordId(entity.getRecordId());
        vo.setLabel(entity.getLabel());
        vo.setConfidence(entity.getConfidence());
        vo.setBbox(entity.getBbox());
        vo.setCount(entity.getCount());
        vo.setAttributes(entity.getAttributes());
        return vo;
    }
}
