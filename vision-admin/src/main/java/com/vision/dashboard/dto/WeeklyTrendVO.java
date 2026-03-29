package com.vision.dashboard.dto;

import lombok.Data;

/**
 * 周趋势数据VO
 */
@Data
public class WeeklyTrendVO {

    /**
     * 日期 (yyyy-MM-dd)
     */
    private String date;

    /**
     * 推理次数
     */
    private Long count;
}
