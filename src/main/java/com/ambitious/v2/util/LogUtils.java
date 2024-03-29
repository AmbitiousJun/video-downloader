package com.ambitious.v2.util;

import com.google.common.collect.Queues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Deque;

/**
 * @author ambitious
 * @date 2023/5/27
 */
public class LogUtils {

    public static final Logger LOGGER = LoggerFactory.getLogger(LogUtils.class);
    public static final String ANSI_INFO = "\u001B[38;2;90;156;248m";
    public static final String ANSI_SUCCESS = "\u001B[38;2;126;192;80m";
    public static final String ANSI_WARNING = "\u001B[38;2;220;165;80m";
    public static final String ANSI_DANGER = "\u001B[38;2;228;116;112m";
    public static final String ANSI_RESET = "\u001B[0m";
    /**
     * 阻塞日志标志
     */
    public static Boolean BLOCK_FLAG = Boolean.FALSE;
    /**
     * 存放日志的队列
     */
    private static final Deque<LogItem> LOG_QUEUE = Queues.synchronizedDeque(Queues.newArrayDeque());
    /**
     * 日志队列的最大长度
     */
    public static final Integer LOG_QUEUE_MAX_LENGTH = Integer.MAX_VALUE / 2;

    /**
     * 将日志队列中的所有日志都清空掉然后退出程序
     */
    public static void flushAndExit() {
        while (!LOG_QUEUE.isEmpty()) {
            SleepUtils.sleep(1000);
        }
        System.exit(-1);
    }

    private static class LogItem {
        Logger logger;
        String type;
        String log;
        public LogItem(Logger logger, String type, String log) {
            this.logger = logger;
            this.type = type;
            this.log = log;
        }
    }

    static {
        // 单独开一个线程输出日志
        new Thread(() -> {
            LOGGER.info("日志输出线程启动成功，开始监听并输出日志...");
            while (true) {
                if (LOG_QUEUE.isEmpty() || BLOCK_FLAG) {
                    // 如果日志阻塞队列长度超出预期的最大值，就抛弃掉旧的日志
                    while (LOG_QUEUE.size() > LOG_QUEUE_MAX_LENGTH) {
                        LOG_QUEUE.pollFirst();
                    }
                    SleepUtils.sleep(1000);
                    continue;
                }
                LogItem item = LOG_QUEUE.pollFirst();
                if (item == null) {
                    continue;
                }
                switch (item.type) {
                    case "warn":
                        item.logger.warn(item.log);
                        break;
                    case "error":
                        item.logger.error(item.log);
                        break;
                    default:
                        item.logger.info(item.log);
                }
            }
        }, "t-log").start();
    }

    /**
     * 当前日志队列中是否还有日志
     * @return 有或无
     */
    public static boolean hasLogs() {
        return !LOG_QUEUE.isEmpty();
    }

    /**
     * 将日志包装成带颜色
     * @param type 颜色前缀，定义于当前类上
     * @param msg 要包装的日志
     * @return 包装完成的日志
     */
    public static String packMsg(String type, String msg) {
        return type + msg + ANSI_RESET;
    }

    public static void info(Logger logger, String msg) {
        LOG_QUEUE.offerLast(new LogItem(logger, "info", packMsg(ANSI_INFO + "\n", msg)));
    }

    public static void success(Logger logger, String msg) {
        LOG_QUEUE.offerLast(new LogItem(logger, "info", packMsg(ANSI_SUCCESS + "\n", msg)));
    }

    public static void warning(Logger logger, String msg) {
        LOG_QUEUE.offerLast(new LogItem(logger, "warn", packMsg(ANSI_WARNING + "\n", msg)));
    }

    public static void error(Logger logger, String msg) {
        LOG_QUEUE.offerLast(new LogItem(logger, "error", packMsg(ANSI_DANGER + "\n", msg)));
    }
}
