package com.ambitious.v2.downloader.threadpool;

import com.ambitious.v2.config.Config;
import com.ambitious.v2.util.LogUtils;
import com.ambitious.v2.util.SleepUtils;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

/**
 * 用于处理下载的线程池
 * @author ambitious
 * @date 2023/5/8
 */
public class DownloadThreadPool {

    private static final ExecutorService SERVICE;
    private static final int CORE_POOL_SIZE;
    private static final int MAXIMUM_POOL_SIZE;
    private static final long KEEP_ALIVE_TIME = 30;
    private static final BlockingQueue<Runnable> WORK_QUEUE = new ArrayBlockingQueue<>(1024 * 1024);
    private static final ThreadFactory FACTORY = new ThreadFactoryBuilder().setNameFormat("download-pool-%d").build();
    public static final Logger LOGGER = LoggerFactory.getLogger(DownloadThreadPool.class);

    static {
        CORE_POOL_SIZE = Config.DOWNLOADER.DL_THREAD_COUNT;
        MAXIMUM_POOL_SIZE = Config.DOWNLOADER.DL_THREAD_COUNT;
        // 初始化线程池
        SERVICE = new ThreadPoolExecutor(
                CORE_POOL_SIZE,
                MAXIMUM_POOL_SIZE,
                KEEP_ALIVE_TIME,
                TimeUnit.SECONDS,
                WORK_QUEUE,
                FACTORY,
                new MyRejectionHandler()
        );
    }

    public static synchronized void exec(Runnable runnable) {
        SERVICE.execute(runnable);
    }
    public static synchronized  <T> Future<T> submit(Callable<T> callable) {
        return SERVICE.submit(callable);
    }
    public static synchronized void submit(Runnable runnable) {
        SERVICE.submit(runnable);
    }

    static class MyRejectionHandler implements RejectedExecutionHandler {

        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            boolean success = false;
            while (!success) {
                LogUtils.warning(LOGGER, "异步任务被拒绝，尝试重新加入队列");
                SleepUtils.sleep(2000);
                success = executor.getQueue().offer(r);
            }
        }
    }
}
