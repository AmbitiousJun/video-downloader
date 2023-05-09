package com.ambitious.v1.downloader.m3u8;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import com.ambitious.v1.downloader.MultiThreadManager;
import com.ambitious.v1.downloader.m3u8.transfer.FileChannelTsTransfer;
import com.ambitious.v1.downloader.m3u8.transfer.TsTransfer;
import com.google.common.collect.Queues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.util.Deque;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 存储基本的 M3U8 下载方法
 * @author ambitious
 * @date 2023/5/1
 */
public class BasicM3U8Downloader {

    /**
     * m3u8 文件后缀
     */
    protected static final String M3U8_SUFFIX = ".m3u8";
    /**
     * mp4 文件后缀
     */
    protected static final String MP4_SUFFIX = ".mp4";
    private final TsTransfer tsTransfer = new FileChannelTsTransfer();

    private static final Logger LOGGER = LoggerFactory.getLogger(BasicM3U8Downloader.class);

    /**
     * 删除临时目录
     * @param tempDir 临时目录
     * @return 是否删除成功
     */
    protected boolean deleteTempDir(File tempDir) {
        // 1 删除子文件
        for (File file : Optional.ofNullable(tempDir.listFiles()).orElse(new File[0])) {
            if (!file.delete()) {
                return false;
            }
        }
        // 2 删除目录
        return tempDir.delete();
    }

    /**
     * 初始化一个存放 ts 文件的临时目录
     * @param baseDir 在哪个目录下创建临时目录
     * @param dirName 临时目录名称
     * @return 临时目录
     */
    protected File initTempDir(File baseDir, String dirName) throws Exception {
        if (!baseDir.exists() && !baseDir.mkdirs()) {
            throw new Exception("无法创建临时目录");
        }
        File dir = new File(baseDir, dirName);
        if (dir.exists() && !dir.delete()) {
            throw new Exception("文件名冲突");
        }
        if (!dir.mkdir()) {
            throw new Exception("无法创建临时目录");
        }
        return dir;
    }

    /**
     * 获取所有的 ts 文件的请求 url
     * @param baseUrl 请求前缀
     * @param url m3u8 文件地址
     * @return 将请求地址封装成列表
     */
    protected Deque<TsMeta> getTsUrls(String baseUrl, String url) throws Exception {
        // 1 读取 m3u8 文件
        InputStream is = new URL(url).openStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        try {
            Deque<TsMeta> res = Queues.newArrayDeque();
            // 2 读取数据，封装对象
            String line = reader.readLine();
            while (line != null) {
                if (line.startsWith("#") || StrUtil.isBlank(line)) {
                    // 去除注释和空行
                    line = reader.readLine();
                    continue;
                }
                String metaUrl = StrUtil.isEmpty(baseUrl) ? line : baseUrl + "/" + line;
                res.offerLast(new TsMeta(metaUrl, res.size() + 1));
                line = reader.readLine();
            }
            return res;
        } finally {
            reader.close();
        }
    }

    protected void handleDownload(M3U8Meta mMeta) throws Exception {
        Deque<TsMeta> tsUrls = mMeta.getTsUrls();
        String fileName = mMeta.getFileName();
        File tempDir = mMeta.getTempDir();
        int totalSegment = tsUrls.size();
        // 请求 ts 文件
        AtomicInteger finishCount = new AtomicInteger(0);
        LOGGER.info("请求 ts 文件中，当前进度：{} / {}，目标视频：{}", finishCount.get(), totalSegment, fileName);
        while (finishCount.get() < totalSegment) {
            if (tsUrls.isEmpty()) {
                // 下载有可能失败，睡眠 2s 后继续循环
                Thread.sleep(2000);
                continue;
            }
            TsMeta meta = tsUrls.pop();
            MultiThreadManager.exec(() -> {
                try {
                    HttpUtil.downloadFile(meta.getUrl(), new File(tempDir, "ts_" + meta.getIndex() + ".ts"));
                    finishCount.incrementAndGet();
                    LOGGER.info("请求 ts 文件中，当前进度：{} / {}，目标视频：{}", finishCount.get(), totalSegment, fileName);
                } catch (Exception e) {
                    // 下载失败，重新加入队列中
                    // LOGGER.info("请求 ts 文件失败，重新加入任务队列中，目标视频：{}", finalFileName);
                    tsUrls.offerLast(meta);
                }
            });
        }
        // 合并 ts 文件
        LOGGER.info("准备将 ts 文件合并成 mp4 文件，目标视频: {}", fileName);
        tsTransfer.ts2Mp4(tempDir, new File(tempDir.getParentFile(), fileName + MP4_SUFFIX));
        LOGGER.info("合并完成，将临时 ts 目录删除，目标视频：{}", fileName);
        if (!deleteTempDir(tempDir)) {
            LOGGER.error("临时目录删除失败，目标视频：{}", fileName);
        }
    }
}
