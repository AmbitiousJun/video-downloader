package com.ambitious.core.downloader.mgm3u8;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import com.ambitious.core.downloader.MultiThreadManager;
import com.ambitious.core.downloader.UrlDownloader;
import com.google.common.collect.Queues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 芒果 TV M3U8 文件下载器（下载所有的 ts 文件之后进行合并）
 * @author ambitious
 * @date 2023/4/29
 */
public class MgM3U8Downloader implements UrlDownloader {

    /**
     * m3u8 文件后缀
     */
    private static final String M3U8_SUFFIX = ".m3u8";
    /**
     * mp4 文件后缀
     */
    private static final String MP4_SUFFIX = ".mp4";
    private static final Logger LOGGER = LoggerFactory.getLogger(MgM3U8Downloader.class);

    @Override
    public void downloadWithoutProgress(String url, File dest) throws Exception {
        if (!url.contains(M3U8_SUFFIX)) {
            throw new Exception("不支持的文件类型");
        }
        String fileName = dest.getName();
        fileName = fileName.substring(0, fileName.length() - M3U8_SUFFIX.length());
        // 1 获取 URL 前缀
        LOGGER.info("正在提取 m3u8 请求前缀，目标视频：{}", fileName);
        String baseUrl = getM3U8BaseUrl(url);
        // 2 获取所有的 ts 文件的请求地址
        LOGGER.info("正在提取所有的 ts 文件请求地址，目标视频：{}", fileName);
        Deque<TsMeta> tsUrls = getTsUrls(baseUrl, url);
        int totalSegment = tsUrls.size();
        if (totalSegment == 0) {
            throw new Exception("ts 文件获取失败");
        }
        // 3 创建存储 ts 文件的临时目录
        File tempDir = initTempDir(dest.getParentFile(), fileName + "_ts_tmp_files");
        LOGGER.info("临时目录创建成功： {}，目标视频：{}", tempDir.getName(), fileName);
        // 4 请求所有的 ts 文件
        AtomicInteger finishCount = new AtomicInteger(0);
        LOGGER.info("请求 ts 文件中，当前进度：{} / {}，目标视频：{}", finishCount.get(), totalSegment, fileName);
        while (finishCount.get() < totalSegment) {
            if (tsUrls.isEmpty()) {
                // 下载有可能失败，睡眠 2s 后继续循环
                Thread.sleep(2000);
                continue;
            }
            TsMeta meta = tsUrls.pop();
            String finalFileName = fileName;
            MultiThreadManager.exec(() -> {
                try {
                    HttpUtil.downloadFile(meta.getUrl(), new File(tempDir, "ts_" + meta.getIndex() + ".ts"));
                    finishCount.incrementAndGet();
                    LOGGER.info("请求 ts 文件中，当前进度：{} / {}，目标视频：{}", finishCount.get(), totalSegment, finalFileName);
                } catch (Exception e) {
                    // 下载失败，重新加入队列中
                    LOGGER.info("请求 ts 文件失败，重新加入任务队列中，目标视频：{}", finalFileName);
                    tsUrls.offerLast(meta);
                }
            });
        }
        // 5 合并 ts 文件
        LOGGER.info("准备将 ts 文件合并成 mp4 文件，目标视频: {}", fileName);
        try (FileOutputStream fos = new FileOutputStream(new File(dest.getParentFile(), fileName + MP4_SUFFIX))) {
            FileChannel outChannel = fos.getChannel();
            for (int i = 1; i <= totalSegment; i++) {
                String tsName = "ts_" + i + ".ts";
                try (FileInputStream fis = new FileInputStream(new File(tempDir, tsName))) {
                    FileChannel inChannel = fis.getChannel();
                    inChannel.transferTo(0, inChannel.size(), outChannel);
                }
            }
        }
        LOGGER.info("合并完成，将临时 ts 目录删除，目标视频：{}", fileName);
        if (!deleteTempDir(tempDir)) {
            LOGGER.error("临时目录删除失败，目标视频：{}", fileName);
        }
    }

    @Override
    public void downloadWithProgress(String url, File dest) throws Exception {
        this.downloadWithoutProgress(url, dest);
    }

    /**
     * 删除临时目录
     * @param tempDir 临时目录
     * @return 是否删除成功
     */
    private boolean deleteTempDir(File tempDir) {
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
    private File initTempDir(File baseDir, String dirName) throws Exception {
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
    private Deque<TsMeta> getTsUrls(String baseUrl, String url) throws Exception {
        // 1 读取 m3u8 文件
        InputStream is = new URL(url).openStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        try {
            Deque<TsMeta> res = Queues.newArrayDeque();
            // 2 读取数据，封装对象
            String line = reader.readLine();
            while (StrUtil.isNotEmpty(line)) {
                if (line.startsWith("#")) {
                    // 去除注释
                    line = reader.readLine();
                    continue;
                }
                res.offerLast(new TsMeta(baseUrl + "/" + line, res.size() + 1));
                line = reader.readLine();
            }
            return res;
        } finally {
            reader.close();
        }
    }

    /**
     * 获取 URL 前缀
     * @param url m3u8 文件 url
     * @return 前缀
     */
    private String getM3U8BaseUrl(String url) {
        // 1 找到后缀的位置
        int suffixPos = url.indexOf(M3U8_SUFFIX);
        // 2 找到后缀之前的第一个 '/'
        int lastSepPos = url.substring(0, suffixPos).lastIndexOf("/");
        return url.substring(0, lastSepPos);
    }

    public static void main(String[] args) throws Exception {
        String url = "https://sluiceyf.titan.mgtv.com/c1/2022/02/08_0/450F42AB184DD6BCB2A7F3E6236E7520_20220208_1_1_2492_mp4/42A6B010E9BA0F4110F8D345EE1974C0.m3u8?arange=0&pm=uRDStBnN2hBNVKu~6drLm7lK~qr8CjeuEJADToNHolC_DOQSQ3oGonAnfWQlQzqYugTm8Mrci8Mz2yQmTSEr8XHjo5NMdF_BIjLOF9WSbtWK8flRH_8uWGzjJhvadxEkB~csmkjCvoMbTLZemT35OBR_6z5vtUOAUwK8Ea591qZsm9BPub5sSypkQAzV1k88svuWO_~0dR4gPaiXVhi1oxcP4AtoWLrsDRHzkT3mBV6EhW2jlidl6t5i__rQky7A9nja0T6FvshmvgSmF7gfdKnjdwBu2VwBA8kdChWwtwrHrdRPz2~ZA_GxXI435D71m3Tnsc8psFetZldPeWAgepBgiPfpSn2d6ZOjUggG9sXiK6rl1SGxWN8B4R0le_oSsJWuWPjwhnPpogMclRt9xQ4ercj_XZh2DZ8qjpJd5Eo_Lmss0d0hwquhm5kuXMIAQlSBbg--&mr=lHfGORbV~gQ~OI5Hzsnlz~pKOVD2lWm9quYaKBSYaNkPgMj0GwIJO4pSfy2WKbeVt8qcu6zQ32oZEe60o~eIb~1NlEgRitshVubu3ZSHMS08aJwHGT2qKPBj~BA-&drmFlag=0&isTrial=0&scid=25117&ruid=a8649f3b26cf4b5f&sh=1";
        MgM3U8Downloader downloader = new MgM3U8Downloader();
        String baseUrl = downloader.getM3U8BaseUrl(url);
        Deque<TsMeta> tsUrls = downloader.getTsUrls(baseUrl, url);
    }

}
