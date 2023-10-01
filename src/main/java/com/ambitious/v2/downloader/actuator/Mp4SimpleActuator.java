package com.ambitious.v2.downloader.actuator;

import cn.hutool.core.io.StreamProgress;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import com.ambitious.v2.pojo.DownloadMeta;
import com.ambitious.v2.util.HttpUtils;
import com.ambitious.v2.util.LogUtils;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 单线程下载 MP4 文件
 * @author ambitious
 * @date 2023/5/8
 */
public class Mp4SimpleActuator implements DownloadActuator{
    
    private static final Logger LOGGER = LoggerFactory.getLogger(Mp4SimpleActuator.class);

    @Override
    @SuppressWarnings("all")
    public void download(DownloadMeta meta) throws Exception {
        OkHttpClient client = new OkHttpClient.Builder()
                .callTimeout(HttpUtils.READ_TIMEOUT, TimeUnit.MILLISECONDS)
                .readTimeout(HttpUtils.READ_TIMEOUT, TimeUnit.MILLISECONDS)
                .build();
        Request request = new Request.Builder()
                .url(meta.getLink())
                .headers(Headers.of(meta.getHeaderMap()))
                .get().build();
        try (Response response = client.newCall(request).execute()) {
            int code = response.code();
            if (code == HttpStatus.HTTP_MOVED_PERM || code == HttpStatus.HTTP_MOVED_PERM) {
                meta.setLink(response.header("Location"));
                download(meta);
            } else if (code != HttpStatus.HTTP_OK) {
                throw new RuntimeException("下载失败，code：" + code);
            }
            File dest = new File(meta.getFileName());
            HttpUtils.downloadStream2File(response.body().byteStream(), dest, Long.valueOf(Optional.ofNullable(response.header("Content-Length")).orElse("0")));
        } catch (Exception e) {
            throw new RuntimeException("下载失败", e);
        }
    }
}
