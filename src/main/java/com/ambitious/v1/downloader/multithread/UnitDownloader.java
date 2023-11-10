package com.ambitious.v1.downloader.multithread;

import com.ambitious.v2.util.HttpUtils;
import okhttp3.Headers;
import okhttp3.Request;

import java.io.File;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 分片下载器
 * @author ambitious
 * @date 2023/4/29
 */
public class UnitDownloader {

    /**
     * 分片起始字节
     */
    private final int from;
    /**
     * 分片结束字节
     */
    private final int to;
    /**
     * 要下载的文件 URL
     */
    private final String url;
    /**
     * 要将文件保存到本地的哪个具体位置
     */
    private final File dest;

    public UnitDownloader(int from, int to, String url, File dest) {
        this.from = from;
        this.to = to;
        this.url = url;
        this.dest = dest;
    }

    /**
     * 下载分片
     */
    public void download(AtomicInteger fileCurSize) throws Exception {
        Request request = new Request.Builder()
                .url(this.url)
                .headers(Headers.of(HttpUtils.genDefaultHeaderMapByUrl(null, this.url)))
                .header("Range", String.format("bytes=%d-%d", this.from, this.to))
                .build();
        HttpUtils.downloadWithRateLimit(request, this.dest);
        fileCurSize.addAndGet(this.to - this.from);
    }
}
