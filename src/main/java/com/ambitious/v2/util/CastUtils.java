package com.ambitious.v2.util;

/**
 * @author ambitious
 * @date 2023/5/6
 */
public class CastUtils {

    @SuppressWarnings("unchecked")
    public static <T> T cast(Object obj) {
        return (T) obj;
    }
}
