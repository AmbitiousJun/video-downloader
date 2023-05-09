package com.ambitious.v1.downloader.m3u8.mgm3u8;

import com.ambitious.v1.downloader.UrlDownloader;
import com.ambitious.v1.downloader.m3u8.BasicM3U8Downloader;
import com.ambitious.v1.downloader.m3u8.M3U8Meta;
import com.ambitious.v1.downloader.m3u8.TsMeta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Deque;

/**
 * 芒果 TV M3U8 文件下载器（下载所有的 ts 文件之后进行合并）
 * @author ambitious
 * @date 2023/4/29
 */
public class MgM3U8Downloader extends BasicM3U8Downloader implements UrlDownloader  {


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
        handleDownload(new M3U8Meta(fileName, tempDir, tsUrls));
    }

    @Override
    public void downloadWithProgress(String url, File dest) throws Exception {
        this.downloadWithoutProgress(url, dest);
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

}
