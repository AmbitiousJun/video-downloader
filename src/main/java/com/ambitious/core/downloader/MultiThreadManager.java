package com.ambitious.core.downloader;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.concurrent.*;

/**
 * 多线程处理分片数据
 * @author ambitious
 * @date 2023/4/28
 */
public class MultiThreadManager {

    /**
     * 线程池
     */
    private static final ExecutorService SERVICE;
    private static final int CORE_POOL_SIZE = 100;
    private static final int MAXIMUM_POOL_SIZE = 100;
    private static final long KEEP_ALIVE_TIME = 30;
    private static final BlockingQueue<Runnable> WORK_QUEUE = new ArrayBlockingQueue<>(10000);
    private static final ThreadFactory FACTORY = new ThreadFactoryBuilder().setNameFormat("download-pool-%d").build();

    static {
        // 初始化线程池
        SERVICE = new ThreadPoolExecutor(
            CORE_POOL_SIZE,
            MAXIMUM_POOL_SIZE,
            KEEP_ALIVE_TIME,
            TimeUnit.SECONDS,
            WORK_QUEUE,
            FACTORY
        );
    }

    public static void exec(Runnable runnable) {
        SERVICE.execute(runnable);
    }

    public static void submit(Callable<?> callable) {
        SERVICE.submit(callable);
    }
}
