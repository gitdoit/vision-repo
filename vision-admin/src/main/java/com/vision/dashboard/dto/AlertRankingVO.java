package com.vision.dashboard.dto;

import lombok.Data;

/**
 * 告警排名VO
 */
@Data
public class AlertRankingVO {

    /**
     * 业务线
     */
    private String businessLine;

    /**
     * 告警数量
     */
    private Long count;
}
