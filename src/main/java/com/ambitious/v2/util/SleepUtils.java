package com.ambitious.v2.util;

/**
 * @author ambitious
 * @date 2023/9/16
 */
public class SleepUtils {

    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {}
    }
}
