package com.vision.common.util;

import java.util.UUID;

/**
 * ID 生成工具类
 *
 * 职责:
 * - 生成标准的 UUID 作为实体主键
 */
public class IdUtil {

    /**
     * 生成 UUID 字符串（无连字符）
     *
     * @return UUID 字符串
     */
    public static String uuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 生成带连字符的 UUID 字符串
     *
     * @return UUID 字符串
     */
    public static String uuidWithHyphen() {
        return UUID.randomUUID().toString();
    }

    private IdUtil() {
    }
}
