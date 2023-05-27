package com.ambitious.v2.util;

import org.slf4j.Logger;

/**
 * @author ambitious
 * @date 2023/5/27
 */
public class LogUtils {

    public static final String ANSI_INFO = "\u001B[38;2;90;156;248m";
    public static final String ANSI_SUCCESS = "\u001B[38;2;126;192;80m";
    public static final String ANSI_WARNING = "\u001B[38;2;220;165;80m";
    public static final String ANSI_DANGER = "\u001B[38;2;228;116;112m";
    public static final String ANSI_RESET = "\u001B[0m";

    public static void info(Logger logger, String msg) {
        logger.info(ANSI_INFO + msg + ANSI_RESET);
    }

    public static void success(Logger logger, String msg) {
        logger.info(ANSI_SUCCESS + msg + ANSI_RESET);
    }

    public static void warning(Logger logger, String msg) {
        logger.warn(ANSI_WARNING + msg + ANSI_RESET);
    }

    public static void error(Logger logger, String msg) {
        logger.error(ANSI_DANGER + msg + ANSI_RESET);
    }
}
