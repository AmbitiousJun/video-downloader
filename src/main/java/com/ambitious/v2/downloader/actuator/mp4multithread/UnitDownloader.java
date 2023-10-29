package com.ambitious.v2.downloader.actuator.mp4multithread;

import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import com.ambitious.v2.util.HttpUtils;
import com.ambitious.v2.util.LogUtils;
import com.ambitious.v2.util.SleepUtils;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 分片下载器
 * @author ambitious
 * @date 2023/4/29
 */
public class UnitDownloader {

    public static final Logger LOGGER = LoggerFactory.getLogger(UnitDownloader.class);

    /**
     * 分片起始字节
     */
    private final long from;
    /**
     * 分片结束字节
     */
    private final long to;
    /**
     * 要下载的文件 URL
     */
    private final String url;
    /**
     * 要将文件保存到本地的哪个具体位置
     */
    private final File dest;

    public UnitDownloader(long from, long to, String url, File dest) {
        this.from = from;
        this.to = to;
        this.url = url;
        this.dest = dest;
    }

    /**
     * 下载分片
     */
    public void download(AtomicLong fileCurSize) throws Exception {
        Map<String, String> defaultHeaders = HttpUtils.genDefaultHeaderMapByUrl(null, this.url);
        Request request = new Request.Builder()
                .url(this.url)
                .headers(Headers.of(defaultHeaders))
                .header("Range", String.format("bytes=%d-%d", this.from, this.to))
                .build();
        while (true) {
            try {
                LogUtils.info(LOGGER, String.format("分片即将开始下载，大小：%.2f MB", 1.0 * (this.to - this.from) / 1024 / 1024));
                HttpUtils.downloadWithRateLimit(request, this.dest);
                fileCurSize.addAndGet(this.to - this.from);
                break;
            } catch (Exception e) {
                LogUtils.warning(LOGGER, String.format("分片下载失败：%s，两秒后重试", e.getMessage()));
                SleepUtils.sleep(2000);
            }
        }
    }
}
