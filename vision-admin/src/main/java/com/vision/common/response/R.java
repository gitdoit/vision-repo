package com.vision.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/**
 * 统一响应结构
 *
 * @param <T> 数据类型
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class R<T> {

    private int code;
    private String message;
    private T data;

    private R() {
    }

    private R(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /**
     * 成功响应（无数据）
     */
    public static <T> R<T> ok() {
        return new R<>(200, "success", null);
    }

    /**
     * 成功响应（带数据）
     */
    public static <T> R<T> ok(T data) {
        return new R<>(200, "success", data);
    }

    /**
     * 成功响应（自定义消息）
     */
    public static <T> R<T> ok(String message, T data) {
        return new R<>(200, message, data);
    }

    /**
     * 失败响应
     */
    public static <T> R<T> fail(String message) {
        return new R<>(400, message, null);
    }

    /**
     * 失败响应（自定义状态码）
     */
    public static <T> R<T> fail(int code, String message) {
        return new R<>(code, message, null);
    }

    /**
     * 服务器错误响应
     */
    public static <T> R<T> error(String message) {
        return new R<>(500, message, null);
    }
}
