package com.ambitious.core.downloader.m3u8.qyf4v;

import com.ambitious.core.MultiThreadDownloader;
import com.ambitious.core.downloader.m3u8.BasicM3U8Downloader;
import com.ambitious.core.downloader.m3u8.M3U8Meta;
import com.ambitious.core.downloader.m3u8.TsMeta;
import com.google.common.collect.Queues;

import java.io.File;
import java.util.Deque;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;

/**
 * 爱奇艺 f4v 文件下载器（适用于下载官方会员视频）
 * @author ambitious
 * @date 2023/5/1
 */
public class QyF4vDownloader extends BasicM3U8Downloader {

    /**
     * 存放将要下载的视频对应的 m3u8 文件的目录
     */
    public static final String M3_U8_DIR = "/Users/ambitious/临时文件/哈哈哈哈哈";
    /**
     * 视频保存地址
     */
    public static final String DOWNLOAD_DIR = "/Users/ambitious/Downloads";

    /**
     * 读取 m3u8 目录列表
     * @return 列表
     */
    public Deque<M3U8Meta> getM3U8Metas() throws Exception {
        File mDir = new File(M3_U8_DIR);
        if (!mDir.exists() || !mDir.isDirectory()) {
            throw new RuntimeException("读取 m3u8 目录失败");
        }
        Deque<M3U8Meta> res = Queues.newArrayDeque();
        for (File file : Optional.ofNullable(mDir.listFiles()).orElse(new File[0])) {
            Deque<TsMeta> tsUrls = getTsUrls("", "file://" + file.getAbsolutePath());
            File tempDir = new File(DOWNLOAD_DIR + "/" + file.getName() + "_tmp_ts_files");
            if (tempDir.exists() && !tempDir.delete()) {
                throw new RuntimeException("无法删除旧临时目录");
            }
            if (!tempDir.mkdirs()) {
                throw new RuntimeException("无法创建临时目录");
            }
            int suffixIdx = file.getName().indexOf(M3U8_SUFFIX);
            if (suffixIdx == -1) {
                throw new RuntimeException("文件名称不合法，请以 " + M3U8_SUFFIX + " 结尾");
            }
            String fileName = file.getName().substring(0, suffixIdx);
            res.offerLast(new M3U8Meta(fileName, tempDir, tsUrls));
        }
        return res;
    }

    public static void main(String[] args) throws Exception {
        QyF4vDownloader downloader = new QyF4vDownloader();
        // 1 读取 m3u8 目录
        Deque<M3U8Meta> metas = downloader.getM3U8Metas();
        // 2 下载
        CountDownLatch latch = new CountDownLatch(metas.size());
        for (M3U8Meta meta : metas) {
            MultiThreadDownloader.exec(() -> {
                try {
                    downloader.handleDownload(meta);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        System.exit(0);
    }
}
