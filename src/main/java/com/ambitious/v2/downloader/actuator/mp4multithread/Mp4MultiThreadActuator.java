package com.ambitious.v2.downloader.actuator.mp4multithread;

import com.ambitious.v1.downloader.MultiThreadManager;
import com.ambitious.v2.downloader.actuator.DownloadActuator;
import com.ambitious.v2.downloader.threadpool.DownloadThreadPool;
import com.ambitious.v2.pojo.DownloadMeta;
import com.ambitious.v2.util.LogUtils;
import com.google.common.collect.Queues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Deque;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 通过手动分片，多线程下载 MP4 文件
 * @author ambitious
 * @date 2023/5/8
 */
public class Mp4MultiThreadActuator implements DownloadActuator {

    /**
     * 分片数
     */
    private static final int SPLIT_COUNT = 8;

    private static final Logger LOGGER = LoggerFactory.getLogger(Mp4MultiThreadActuator.class);

    @Override
    @SuppressWarnings("all")
    public void download(DownloadMeta meta) throws Exception {
        final File dest = new File(meta.getFileName());
        final String fileName = dest.getName();
        final AtomicInteger fileTotalSize = new AtomicInteger(0);
        final AtomicInteger fileCurSize = new AtomicInteger(0);
        final Deque<UnitTask> taskList = Queues.newArrayDeque();
        // 使用定时器记录下载进度
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
            if (!dest.exists() || fileTotalSize.get() == 0) {
                return;
            }
            // 1 获取文件当前大小
            BigDecimal curSize = new BigDecimal(fileCurSize.get());
            BigDecimal totalSize = new BigDecimal(fileTotalSize.get());
            // 2 计算百分比
            BigDecimal percent = curSize.divide(totalSize, new MathContext(4, RoundingMode.HALF_UP)).multiply(new BigDecimal(100));
            LogUtils.info(LOGGER, String.format("下载进度：%s%，文件名：%s", percent, fileName));
            if (curSize.compareTo(totalSize) >= 0) {
                // 文件已经下载完成
                timer.cancel();
            }
            }
        }, 0, 10000);

        LogUtils.info(LOGGER, String.format("开始下载，文件名：%s", fileName));
        HttpURLConnection conn = null;
        try {
            // 1 初始化文件
            initFile(dest);
            // 2 获取文件总大小
            conn = (HttpURLConnection) new URL(meta.getLink()).openConnection();
            conn.connect();
            fileTotalSize.set(conn.getContentLength());
            // 3 初始化任务列表
            initTaskList(fileTotalSize.get(), taskList);
            // 4 分片并使用多线程进行下载
            final AtomicInteger finishCount = new AtomicInteger(0);
            while (finishCount.get() < SPLIT_COUNT) {
                if (taskList.isEmpty()) {
                    // 任务有可能失败，先睡眠再重新判断
                    Thread.sleep(2000);
                    continue;
                }
                UnitTask task = taskList.pop();
                DownloadThreadPool.exec(() -> {
                    try {
                        new UnitDownloader(task.getFrom(), task.getTo(), meta.getLink(), dest).download(fileCurSize);
                        finishCount.incrementAndGet();
                    } catch (Exception e) {
                        LogUtils.error(LOGGER, "分片下载失败，重新加入任务列表");
                        taskList.offerLast(task);
                    }
                });
            }
            LOGGER.info("下载完成，文件名：{}", fileName);
        } catch (Exception e) {
            dest.delete();
            throw new Exception("文件下载失败");
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * 初始化任务列表
     */
    private void initTaskList(int fileTotalSize, Deque<UnitTask> taskList) {
        int unitSize = fileTotalSize / SPLIT_COUNT;
        for (int i = 0; i < SPLIT_COUNT; i++) {
            int from = i * unitSize;
            // 最后一片直接取所有
            int to = i == SPLIT_COUNT - 1 ? fileTotalSize : (i + 1) * unitSize;
            taskList.offerLast(new UnitTask(from, to));
        }
    }

    /**
     * 如果文件不存在，先创建一个
     * @param file 要初始化的文件
     */
    private void initFile(File file) throws IOException {
        if (file.exists()) {
            return;
        }
        // 如果有多级目录，先创建出缺失的目录
        if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
            throw new RuntimeException("文件创建失败");
        }
        if (!file.createNewFile()) {
            throw new RuntimeException("文件创建失败");
        }
    }
}
