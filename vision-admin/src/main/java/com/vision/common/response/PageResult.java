package com.vision.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 分页结果
 *
 * @param <T> 列表项类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PageResult<T> {

    /**
     * 列表数据
     */
    private List<T> items;

    /**
     * 总数
     */
    private Long total;

    /**
     * 创建分页结果
     */
    public static <T> PageResult<T> of(List<T> items, Long total) {
        return new PageResult<>(items, total);
    }

    /**
     * 创建空分页结果
     */
    public static <T> PageResult<T> empty() {
        return new PageResult<>(List.of(), 0L);
    }
}
