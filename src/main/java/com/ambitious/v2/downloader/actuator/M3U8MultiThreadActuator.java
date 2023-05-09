package com.ambitious.v2.downloader.actuator;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.ambitious.v2.constant.FileConstant;
import com.ambitious.v2.downloader.threadpool.DownloadThreadPool;
import com.ambitious.v2.pojo.DownloadMeta;
import com.ambitious.v2.pojo.TsMeta;
import com.ambitious.v2.util.HttpUtils;
import com.ambitious.v2.util.M3U8Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Deque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * m3u8 多线程下载执行期
 * @author ambitious
 * @date 2023/5/9
 */
public class M3U8MultiThreadActuator extends M3U8Actuator {

    private static final Logger LOGGER = LoggerFactory.getLogger(M3U8MultiThreadActuator.class);

    @Override
    protected void handleDownload(DownloadMeta meta, Deque<TsMeta> tsMetas, File tempDir) throws InterruptedException {
        AtomicInteger finish = new AtomicInteger(0);
        int size = tsMetas.size();
        while (finish.get() < size) {
            if (tsMetas.isEmpty()) {
                // 可能会有下载失败的 ts 文件，先阻塞两秒
                Thread.sleep(2000);
                continue;
            }
            TsMeta tsMeta = tsMetas.pop();
            DownloadThreadPool.submit(() -> {
                try {
                    coreDownload(meta, tsMeta, tempDir);
                    LOGGER.info("请求 ts 文件中，当前进度：{} / {}，目标视频：{}", finish.get(), size, meta.getFileName());
                    finish.incrementAndGet();
                } catch (Exception e) {
                    LOGGER.error("请求失败，重新加入到队列中，目标视频：{}", meta.getFileName());
                    tsMetas.offerLast(tsMeta);
                }
            });
        }
        LOGGER.info("ts 列表下载完成");
    }
}
