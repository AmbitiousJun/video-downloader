package com.ambitious.v2;

import cn.hutool.core.util.StrUtil;
import com.ambitious.v2.config.Config;
import com.ambitious.v2.decoder.Decoder;
import com.ambitious.v2.downloader.Downloader;
import com.ambitious.v2.pojo.DownloadMeta;
import com.ambitious.v2.pojo.VideoMeta;
import com.google.common.collect.Queues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Deque;
import java.util.concurrent.CountDownLatch;

/**
 * @author ambitious
 * @date 2023/5/4
 */
public class Main {

    /**
     * 待解析列表
     */
    public static final Deque<VideoMeta> DECODE_LIST = Queues.newArrayDeque();
    /**
     * 待下载列表
     */
    public static final Deque<DownloadMeta> DOWNLOAD_LIST = Queues.newArrayDeque();

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws InterruptedException {
        // 1 初始化配置
        Config.load();
        // 2 读取要处理的视频数据
        readVideoData();
        int size = DECODE_LIST.size();
        if (size == 0) {
            LOGGER.info("解析列表为空，程序停止");
            return;
        }
        // 3 开启解析任务
        Decoder.listenAndDecode(DECODE_LIST, DOWNLOAD_LIST::offerLast);
        // 4 开启下载任务
        CountDownLatch latch = new CountDownLatch(size);
        Downloader.listenAndDownload(DOWNLOAD_LIST, () -> {
            latch.countDown();
            LOGGER.info("一个文件下载完成，剩下：{} 个", latch.getCount());
        });
        latch.await();
        LOGGER.info("所有任务处理完成");
        System.exit(0);
    }

    /**
     * 读取需要下载的视频源数据文件
     */
    public static void readVideoData() {
        LOGGER.info("正在读取源数据文件 data.txt...");
        // 1 将文件 data.txt 读取成字符输入流
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(Paths.get("config/data.txt"))))) {
            // 2 依次读取每一行数据，实例化对象
            String line = reader.readLine();
            while (StrUtil.isNotBlank(line)) {
                if (!line.contains("|")) {
                    throw new RuntimeException("文件格式不合法，请遵循：文件名|地址");
                }
                String[] data = line.split("\\|");
                if (data.length != 2) {
                    throw new RuntimeException("文件格式不合法，请遵循：文件名|地址");
                }
                VideoMeta videoMeta = new VideoMeta(data[0], data[1]);
                DECODE_LIST.offerLast(videoMeta);
                line = reader.readLine();
            }
            for (VideoMeta meta : DECODE_LIST) {
                System.out.println(meta);
            }
            LOGGER.info("读取完成！");
        } catch (Exception e) {
            throw new RuntimeException("读取源数据文件失败", e);
        }
    }
}
