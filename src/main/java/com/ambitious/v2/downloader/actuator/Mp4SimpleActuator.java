package com.ambitious.v2.downloader.actuator;

import cn.hutool.core.io.StreamProgress;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import com.ambitious.v2.pojo.DownloadMeta;
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
        Request request = new Request.Builder()
                .url(meta.getLink())
                .headers(Headers.of(HttpUtils.genDefaultHeaderMapByUrl(meta.getHeaderMap(), meta.getLink())))
                .get().build();
        File dest = new File(meta.getFileName());
        try {
            LogUtils.info(LOGGER, "开始下载：" + meta.getFileName());
            HttpUtils.downloadWithRateLimit(request, dest);
            LogUtils.success(LOGGER, "下载完成：" + meta.getFileName());
        } catch (Exception e) {
            LogUtils.warning(LOGGER, "下载异常，两秒后重试");
            SleepUtils.sleep(2000);
            download(meta);
        }
    }
}
