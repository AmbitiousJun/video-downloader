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
import com.ambitious.v2.util.LogUtils;
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
    private static final String M3U8_ERROR = "不是规范的 m3u8 文件";

    /**
     * 监听任务列表并下载
     * @param list 下载任务列表
     */
    @SuppressWarnings("all")
    public static void listenAndDownload(
        Deque<DownloadMeta> list,
        DownloadListener listener,
        DownloadErrorListener errorListener
    ) {
        new Thread(() -> {
            LogUtils.info(LOGGER, "开始监听下载列表...");
            while (true) {
                while (list.isEmpty()) {
                    // 每隔两秒检查一下下载线程
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                final DownloadMeta meta = list.pop();
                DownloadTaskThreadPool.submit(() -> {
                    String originFileName = meta.getFileName();
                    try {
                        String link = meta.getLink();
                        String fileName = Config.DOWNLOADER.DOWNLOAD_DIR + "/" + originFileName + ".mp4";
                        meta.setFileName(fileName);
                        LogUtils.info(LOGGER, String.format("监听到下载任务，文件名：%s，下载地址：%s", fileName, link));
                        initActuator();
                        actuator.download(meta);
                        listener.completeOne();
                    } catch (Exception e) {
                        meta.setFileName(originFileName);
                        if (e.getMessage().equals(UN_SUPPORT_MSG)) {
                            throw new RuntimeException("下载失败", e);
                        } else if (e.getMessage().equals(M3U8_ERROR)) {
                            // m3u8 文件无法正常解析
                            errorListener.emit(meta);
                            LogUtils.warning(LOGGER, "重新添加到解析任务中。视频名称：" + meta.getFileName());
                        } else {
                            LogUtils.error(LOGGER, "文件下载失败，重新加入任务列表：" + e.getMessage());
                            synchronized (Downloader.class) {
                                list.offerLast(meta);
                            }
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
