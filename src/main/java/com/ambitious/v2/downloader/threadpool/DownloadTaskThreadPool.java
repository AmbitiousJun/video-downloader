package com.ambitious.v2.downloader.threadpool;

import com.ambitious.v2.config.Config;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.concurrent.*;

/**
 * 用于处理下载任务的线程池
 * @author ambitious
 * @date 2023/5/8
 */
public class DownloadTaskThreadPool {

    private static final ExecutorService SERVICE;
    private static final int CORE_POOL_SIZE;
    private static final int MAXIMUM_POOL_SIZE;
    private static final long KEEP_ALIVE_TIME = 30;
    private static final BlockingQueue<Runnable> WORK_QUEUE = new ArrayBlockingQueue<>(100);
    private static final ThreadFactory FACTORY = new ThreadFactoryBuilder().setNameFormat("download-task-pool-%d").build();

    static {
        CORE_POOL_SIZE = Config.DOWNLOADER.TASK_THREAD_COUNT;
        MAXIMUM_POOL_SIZE = Config.DOWNLOADER.TASK_THREAD_COUNT + 10;
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

    public static synchronized void exec(Runnable runnable) {
        SERVICE.execute(runnable);
    }
    public static synchronized <T> Future<T> submit(Callable<T> callable) {
        return SERVICE.submit(callable);
    }
    public static synchronized void submit(Runnable runnable) {
        SERVICE.submit(runnable);
    }
}
