package com.ambitious.v1;

import cn.hutool.core.util.StrUtil;
import com.ambitious.v1.downloader.UrlDownloader;
import com.ambitious.v1.downloader.m3u8.mgm3u8.MgM3U8Downloader;
import com.google.common.collect.Queues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Collection;
import java.util.Deque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * @author ambitious
 * @date 2023/4/28
 */
public class Main {

    /**
     * 存放从 data.txt 文件中读取到的源视频数据
     */
    private static final Deque<VideoMeta> ORIGIN_VIDEOS = Queues.newArrayDeque();
    /**
     * 存放待下载的视频
     */
    private static final Deque<DownloadMeta> DOWNLOAD_VIDEOS = Queues.newArrayDeque();
    /**
     * 视频要下载到哪里
     */
    private static final String DOWNLOAD_DIR = "/Users/ambitious/Downloads";
    /**
     * 视频文件后缀
     */
    private static final String SUFFIX = ".m3u8";
    /**
     * 解析链接
     */
    private static final String BASE_URL = DecodeBaseUrl.XIA_MI;
    /**
     * 等待解析的时间（秒）
     */
    private static final int WAIT_SECONDS = 30;
    /**
     * 下载失败重试次数
     */
    private static final int RETRY_TIMES = 6;
    public static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws InterruptedException {
        // 读取任务列表
        readVideoData();
        int size = ORIGIN_VIDEOS.size();
        if (size < 1) {
            LOGGER.info("任务列表为空，程序停止");
            return;
        }
        LOGGER.info("开始下载 " + size + " 个视频文件");
        CountDownLatch latch = new CountDownLatch(size);
        // 监听并处理下载列表
        listenDownload(latch, size);
        // 监听并处理解析任务列表
        listenDecode(size);
        latch.await();
        LOGGER.info("所有视频下载完成！");
        System.exit(0);
    }

    /**
     * 开启一个新线程，监听解析列表
     * @param size
     */
    @SuppressWarnings("all")
    public static void listenDecode(int size) {
        new Thread(() -> {
            LOGGER.info("开始监听解析列表");
            final AtomicInteger finish = new AtomicInteger(0);
            while (true) {
                while (ORIGIN_VIDEOS.size() == 0) {
                    if (finish.get() == size) {
                        LOGGER.info("所有文件已解析完成，请耐心等待下载结束...");
                        Thread.interrupted();
                        return;
                    }
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                VideoMeta meta = ORIGIN_VIDEOS.pop();
                String name = meta.getName();
                String url = meta.getUrl();
                LOGGER.info("检测到解析任务，标题：{}，源地址：{}", name, url);
                CountDownLatch latch = new CountDownLatch(1);
                MultiThreadDecoder.exec(() -> {
                    MySelenium client = new MySelenium();
                    try {
                        LOGGER.info("开始解析视频，标题：{}, 源地址：{}", name, url);
                        // 1 创建客户端
                        String finalUrl = BASE_URL + url;
                        String link = client.fetchDownloadLink(finalUrl, WAIT_SECONDS);
                        String fileName = DOWNLOAD_DIR + "/" + name + SUFFIX;
                        LOGGER.info("解析成功，已添加到下载列表，文件名：{}，下载地址：{}", fileName, link);
                        // 添加到下载列表
                        DOWNLOAD_VIDEOS.offerLast(new DownloadMeta(link, fileName));
                        int current = finish.incrementAndGet();
                        LOGGER.info("已解析完成视频数：{}", current);
                    } catch (Exception e) {
                        LOGGER.error("视频下载地址解析失败，重新加入任务列表");
                        ORIGIN_VIDEOS.offerLast(meta);
                    } finally {
                        client.destroy();
                        latch.countDown();
                    }
                });
            }
        }, "t-decode-listening").start();
    }

    /**
     * 开启一个新线程，监听下载列表
     */
    @SuppressWarnings("all")
    public static void listenDownload(CountDownLatch latch, int size) {
        new Thread(() -> {
            LOGGER.info("开始监听下载列表...");
            final AtomicInteger finish = new AtomicInteger(0);
            while (true) {
                while (DOWNLOAD_VIDEOS.size() == 0) {
                    if (finish.get() == size) {
                        LOGGER.info("所有文件下载完成");
                        Thread.interrupted();
                        return;
                    }
                    // 每隔两秒检查一下下载线程
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                DownloadMeta meta = DOWNLOAD_VIDEOS.pop();
                String link = meta.getLink();
                String fileName = meta.getFileName();
                LOGGER.info("监听到下载任务，文件名：{}，下载地址：{}", fileName, link);
                MultiThreadDownloader.exec(() -> {
                    UrlDownloader downloader = new MgM3U8Downloader();
                    int times = 1;
                    while (times <= RETRY_TIMES) {
                        try {
                            LOGGER.info("即将开始下载，文件名：{}，第 {} 次尝试，下载地址：{}", fileName, times, link);
                            downloader.downloadWithProgress(link, new File(fileName));
                            break;
                        } catch (Exception e) {
                            LOGGER.info("文件 [{}] 下载失败", fileName);
                            times++;
                        }
                    }
                    if (times > RETRY_TIMES) {
                        LOGGER.error("文件下载失败，重新加入任务列表");
                        DOWNLOAD_VIDEOS.offerLast(meta);
                    } else {
                        latch.countDown();
                        int current = finish.incrementAndGet();
                        LOGGER.info("剩下 {} 个文件未完成", size - current);
                    }
                });
            }
        }, "t-download-listening").start();
    }

    /**
     * 读取需要下载的视频源数据文件
     */
    public static void readVideoData() {
        LOGGER.info("正在读取源视频文件 data.txt...");
        // 1 将文件 data.txt 读取成字符输入流
        File file = new File("data.txt");
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            // 2 依次读取每一行数据，实例化对象
            String line = reader.readLine();
            while (StrUtil.isNotBlank(line)) {
                if (!line.contains("|")) {
                    LOGGER.error("文件格式不合法，请遵循：文件名|地址");
                    System.exit(1);
                }
                String[] data = line.split("\\|");
                if (data.length != 2) {
                    LOGGER.error("文件格式不合法，请遵循：文件名|地址");
                    System.exit(1);
                }
                VideoMeta videoMeta = new VideoMeta(data[0], data[1]);
                ORIGIN_VIDEOS.offerLast(videoMeta);
                line = reader.readLine();
            }
            printList(ORIGIN_VIDEOS);
            LOGGER.info("读取完成！");
        } catch (Exception e) {
            LOGGER.error("读取失败");
            throw new RuntimeException(e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    LOGGER.error("资源关闭失败");
                    throw new RuntimeException(e);
                }
            }
        }
    }

    /**
     * 打印 List 到控制台
     * @param list 要打印的列表
     */
    private static void printList(Collection<?> list) {
        for (Object obj : list) {
            System.out.println(obj);
        }
    }
}
