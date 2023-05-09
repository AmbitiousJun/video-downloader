package com.ambitious.v2.util;

import cn.hutool.core.util.NumberUtil;

/**
 * @author ambitious
 * @date 2023/5/4
 */
public class NumberUtils {

    /**
     * 字符串转整形
     */
    public static int a2i(String val) {
        return NumberUtil.isInteger(val) ? Integer.parseInt(val) : -1;
    }
}
