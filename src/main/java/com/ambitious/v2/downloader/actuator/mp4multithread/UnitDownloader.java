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
        // 按照 200 KB/s 的下载速度计算超时时间
        int timeout = Math.max(5 * 60 * 1000, (this.to - this.from) / 1024 / 200 * 1000);
        OkHttpClient client = new OkHttpClient.Builder()
                .readTimeout(timeout, TimeUnit.MILLISECONDS)
                .callTimeout(timeout, TimeUnit.MILLISECONDS)
                .build();
        Map<String, String> defaultHeaders = HttpUtils.genDefaultHeaderMapByUrl(null, this.url);
        Request request = new Request.Builder()
                .url(this.url)
                .headers(Headers.of(defaultHeaders))
                .header("Range", String.format("bytes=%d-%d", this.from, this.to))
                .build();
        while (true) {
            try (Response response = client.newCall(request).execute()) {
                if (response.body() == null) {
                    throw new RuntimeException("响应体为空");
                }
                if (!response.isSuccessful()) {
                    throw new RuntimeException(response.body().string());
                }
                LogUtils.info(LOGGER, String.format("分片即将开始下载，超时时间为：%d 秒", timeout / 1000));
                InputStream is = response.body().byteStream();
                long streamLength = response.body().contentLength();
                LogUtils.info(LOGGER, String.format("分片实际大小：%.2f MB", Long.valueOf(streamLength).doubleValue() / 1024 / 1024));
                try (RandomAccessFile file = new RandomAccessFile(this.dest, "rw")) {
                    // 定位到文件中该分片的位置
                    file.seek(this.from);
                    // 缓冲区
                    byte[] buffer = new byte[Long.valueOf(streamLength).intValue() / 2 + 1];
                    int len = is.read(buffer, 0, buffer.length);
                    while (len > 0) {
                        file.write(buffer, 0, len);
                        len = is.read(buffer, 0, buffer.length);
                    }
                    fileCurSize.addAndGet(this.to - this.from);
                }
                return;
            } catch (Exception e) {
                LogUtils.warning(LOGGER, String.format("分片下载失败：%s，两秒后重试", e.getMessage()));
                SleepUtils.sleep(2000);
            }
        }
    }
}
