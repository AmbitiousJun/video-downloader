package com.ambitious.core.downloader.multithread;

import com.ambitious.core.downloader.MultiThreadManager;
import com.ambitious.core.downloader.UrlDownloader;
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
 * 自定义多线程下载器
 * @author ambitious
 * @date 2023/4/29
 */
public class MultiThreadUrlDownloader implements UrlDownloader {

    /**
     * 每个文件分成 8 片来下载
     */
    private static final int SPLIT_COUNT = 8;
    /**
     * 文件大小
     */
    private int fileTotalSize = 0;
    /**
     * 实时记录已下载文件的大小
     */
    private final AtomicInteger fileCurSize = new AtomicInteger(0);
    private final Deque<UnitTask> taskList = Queues.newArrayDeque();
    private static final Logger LOGGER = LoggerFactory.getLogger(MultiThreadUrlDownloader.class);

    @Override
    @SuppressWarnings("all")
    public void downloadWithoutProgress(String url, File dest) throws Exception {
        String fileName = dest.getName();
        LOGGER.info("开始下载，文件名：{}", fileName);
        HttpURLConnection conn = null;
        try {
            // 1 初始化文件
            initFile(dest);
            // 2 获取文件总大小
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.connect();
            fileTotalSize = conn.getContentLength();
            // 3 初始化任务列表
            initTaskList();
            // 4 分片并使用多线程进行下载
            final AtomicInteger finishCount = new AtomicInteger(0);
            while (true) {
                if (finishCount.get() >= SPLIT_COUNT) {
                    break;
                }
                if (taskList.isEmpty()) {
                    // 任务有可能失败，先睡眠再重新判断
                    Thread.sleep(2000);
                    continue;
                }
                UnitTask task = taskList.pop();
                MultiThreadManager.exec(() -> {
                    try {
                        new UnitDownloader(task.getFrom(), task.getTo(), url, dest).download(fileCurSize);
                        finishCount.incrementAndGet();
                    } catch (Exception e) {
                        // 下载失败，重新加到下载列表
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

    @Override
    @SuppressWarnings("all")
    public void downloadWithProgress(String url, File dest) throws Exception {
        String fileName = dest.getName();
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (!dest.exists() || fileTotalSize == 0) {
                    return;
                }
                // 1 获取文件当前大小
                BigDecimal curSize = new BigDecimal(fileCurSize.get());
                BigDecimal totalSize = new BigDecimal(fileTotalSize);
                // 2 计算百分比
                BigDecimal percent = curSize.divide(totalSize, new MathContext(4, RoundingMode.HALF_UP)).multiply(new BigDecimal(100));
                LOGGER.info("下载进度：{}%，文件名：{}", percent, fileName);
                if (curSize.compareTo(totalSize) >= 0) {
                    // 文件已经下载完成
                    timer.cancel();
                }
            }
        }, 0, 10000);
        this.downloadWithoutProgress(url, dest);
    }

    /**
     * 初始化任务列表
     */
    private void initTaskList() {
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
