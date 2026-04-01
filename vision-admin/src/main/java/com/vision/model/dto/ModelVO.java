package com.vision.model.dto;

import com.vision.model.entity.Model;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 模型视图对象
 */
@Data
public class ModelVO {

    private String id;
    private String name;
    private String version;
    private String businessTag;
    private String taskType;
    private List<String> engineSupport;
    private String targetHardware;
    private List<String> classNames;
    private Integer numClasses;
    private String parsedStatus;
    private BigDecimal confidenceThreshold;
    private String inputResolution;
    private Integer maxConcurrency;
    private String modelPath;
    private String author;
    private Integer avgLatency;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 当前部署列表（模型加载到了哪些节点） */
    private List<ModelDeploymentVO> deployments;

    /**
     * 从实体转换
     */
    public static ModelVO fromEntity(Model entity) {
        ModelVO vo = new ModelVO();
        vo.setId(entity.getId());
        vo.setName(entity.getName());
        vo.setVersion(entity.getVersion());
        vo.setBusinessTag(entity.getBusinessTag());
        vo.setTaskType(entity.getTaskType());
        vo.setEngineSupport(
                entity.getEngineSupport() != null && !entity.getEngineSupport().isEmpty()
                        ? Arrays.asList(entity.getEngineSupport().split(","))
                        : Collections.emptyList()
        );
        vo.setTargetHardware(entity.getTargetHardware());
        vo.setClassNames(
                entity.getClassNames() != null && !entity.getClassNames().isEmpty()
                        ? Arrays.asList(entity.getClassNames().split(","))
                        : Collections.emptyList()
        );
        vo.setNumClasses(entity.getNumClasses());
        vo.setParsedStatus(entity.getParsedStatus());
        vo.setConfidenceThreshold(entity.getConfidenceThreshold());
        vo.setInputResolution(entity.getInputResolution());
        vo.setMaxConcurrency(entity.getMaxConcurrency());
        vo.setModelPath(entity.getModelPath());
        vo.setAuthor(entity.getAuthor());
        vo.setAvgLatency(entity.getAvgLatency());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }
}
