package com.ambitious.v2.downloader;

import com.ambitious.v2.config.Config;
import com.ambitious.v2.constant.DownloaderType;
import com.ambitious.v2.constant.MediaType;
import com.ambitious.v2.downloader.actuator.DownloadActuator;
import com.ambitious.v2.downloader.actuator.M3U8MultiThreadActuator;
import com.ambitious.v2.downloader.actuator.M3U8SimpleActuator;
import com.ambitious.v2.downloader.actuator.Mp4SimpleActuator;
import com.ambitious.v2.downloader.actuator.mp4multithread.Mp4MultiThreadActuator;
import com.ambitious.v2.downloader.threadpool.DownloadTaskThreadPool;
import com.ambitious.v2.pojo.DownloadMeta;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Deque;
import java.util.Map;

/**
 * 统一下载入口
 * @author ambitious
 * @date 2023/5/8
 */
public class Downloader {

    private static final Logger LOGGER = LoggerFactory.getLogger(Downloader.class);
    private volatile static DownloadActuator actuator;
    private static final String UN_SUPPORT_MSG = "不支持的下载方式";

    /**
     * 监听任务列表并下载
     * @param list 下载任务列表
     */
    @SuppressWarnings("all")
    public static void listenAndDownload(Deque<DownloadMeta> list, DownloadListener listener) {
        new Thread(() -> {
            LOGGER.info("开始监听下载列表...");
            while (true) {
                while (list.isEmpty()) {
                    // 每隔两秒检查一下下载线程
                    try {
                        LOGGER.info("当前没有下载任务，监听线程阻塞中...");
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                final DownloadMeta meta = list.pop();
                DownloadTaskThreadPool.submit(() -> {
                    try {
                        String link = meta.getLink();
                        String fileName = meta.getFileName();
                        LOGGER.info("监听到下载任务，文件名：{}，下载地址：{}", fileName, link);
                        initActuator();
                        actuator.download(meta);
                        listener.completeOne();
                    } catch (Exception e) {
                        if (e.getMessage().equals(UN_SUPPORT_MSG)) {
                            throw new RuntimeException("下载失败", e);
                        }
                        LOGGER.error("文件下载失败，重新加入任务列表", e);
                        synchronized (Downloader.class) {
                            list.offerLast(meta);
                        }
                    }
                });
            }
        }, "t-download-listening").start();
    }

    /**
     * 初始化下载执行器
     */
    private static void initActuator() throws InstantiationException, IllegalAccessException {
        if (actuator != null) {
            return;
        }
        synchronized (Downloader.class) {
            if (actuator != null) {
                return;
            }
            MediaType rt = Config.DECODER.RESOURCE_TYPE;
            DownloaderType use = Config.DOWNLOADER.USE;
            Map<String, Class<? extends DownloadActuator>> checkMap = Maps.newHashMap();
            checkMap.put(MediaType.MP4.value + DownloaderType.SIMPLE.value, Mp4SimpleActuator.class);
            checkMap.put(MediaType.MP4.value + DownloaderType.MULTI_THREAD.value, Mp4MultiThreadActuator.class);
            checkMap.put(MediaType.M3U8.value + DownloaderType.SIMPLE.value, M3U8SimpleActuator.class);
            checkMap.put(MediaType.M3U8.value + DownloaderType.MULTI_THREAD.value, M3U8MultiThreadActuator.class);
            if (!checkMap.containsKey(rt.value + use.value)) {
                throw new RuntimeException(UN_SUPPORT_MSG);
            }
            actuator = checkMap.get(rt.value + use.value).newInstance();
        }
    }
}
