package com.ambitious.v2.downloader.actuator.mp4multithread;

import cn.hutool.http.HttpStatus;
import com.ambitious.v2.downloader.actuator.DownloadActuator;
import com.ambitious.v2.downloader.threadpool.DownloadThreadPool;
import com.ambitious.v2.pojo.DownloadMeta;
import com.ambitious.v2.util.HttpUtils;
import com.ambitious.v2.util.LogUtils;
import com.ambitious.v2.util.SleepUtils;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
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
import java.util.concurrent.atomic.AtomicLong;

/**
 * 通过手动分片，多线程下载 MP4 文件
 * @author ambitious
 * @date 2023/5/8
 */
public class Mp4MultiThreadActuator implements DownloadActuator {

    private static final Logger LOGGER = LoggerFactory.getLogger(Mp4MultiThreadActuator.class);

    private static final Integer SPLIT_COUNT = 64;

    @Override
    @SuppressWarnings("all")
    public void download(DownloadMeta meta) throws Exception {
        final File dest = new File(meta.getFileName());
        final String fileName = dest.getName();
        final AtomicLong fileTotalSize = new AtomicLong(0L);
        final AtomicLong fileCurSize = new AtomicLong(0L);
        final Deque<UnitTask> taskList = Queues.newArrayDeque();
        LogUtils.info(LOGGER, String.format("开始下载，文件名：%s", fileName));
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(meta.getLink())
                .headers(Headers.of(HttpUtils.genDefaultHeaderMapByUrl(null, meta.getLink())))
                .header("Connection", "Close")
                .build();
        try (Response response = client.newCall(request).execute()) {
            // 1 初始化文件
            initFile(dest);
            // 2 获取文件总大小
            int code = response.code();
            if (code != HttpStatus.HTTP_OK) {
                throw new RuntimeException("请求失败，status: " + code);
            }
            fileTotalSize.set(response.body().contentLength());
            // 3 初始化任务列表
            initTaskList(fileTotalSize.get(), taskList);
            // 4 分片并使用多线程进行下载
            final AtomicInteger finishCount = new AtomicInteger(0);
            int splitCount = taskList.size();
            while (finishCount.get() < splitCount) {
                if (taskList.isEmpty()) {
                    // 任务有可能失败，先睡眠再重新判断
                    SleepUtils.sleep(2000);
                    continue;
                }
                UnitTask task = taskList.pop();
                DownloadThreadPool.exec(() -> {
                    try {
                        new UnitDownloader(task.getFrom(), task.getTo(), meta.getLink(), dest).download(fileCurSize);
                        finishCount.incrementAndGet();
                        LogUtils.info(LOGGER, String.format("下载进度：%d / %d，文件名：%s", finishCount.get(), splitCount, fileName));
                    } catch (Exception e) {
                        LogUtils.error(LOGGER, String.format("分片下载失败：%s，重新加入任务列表，文件名：%s", e.getMessage(), fileName));
                        synchronized (taskList) {
                            taskList.offerLast(task);
                        }
                    }
                });
            }
            LogUtils.success(LOGGER, "下载完成");
        } catch (Exception e) {
            dest.delete();
            throw new Exception("文件下载失败：" + e.getMessage());
        }
    }

    /**
     * 初始化任务列表
     */
    private void initTaskList(long fileTotalSize, Deque<UnitTask> taskList) {
        long size = (long) Math.ceil(1.0 * fileTotalSize / SPLIT_COUNT);
        // 每个分片大小 2 ～ 4 MB
        long baseSize = 2 * 1024 * 1024;
        for (int i = 0; i < SPLIT_COUNT; i++) {
            long curSize = Math.min(size, fileTotalSize - i * size);
            long start = i * curSize;
            while (curSize > 2 * baseSize) {
                long random = (long) (baseSize * Math.random());
                long to = start + baseSize + random;
                taskList.offerLast(new UnitTask(start, to));
                curSize -= (baseSize + random);
                start = to;
            }
            if (curSize > 0) {
                taskList.offerLast(new UnitTask(start, start + curSize));
            }
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
