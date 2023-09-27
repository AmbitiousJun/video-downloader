package com.ambitious.v2.downloader.actuator;

import com.ambitious.v2.downloader.threadpool.DownloadThreadPool;
import com.ambitious.v2.pojo.DownloadMeta;
import com.ambitious.v2.pojo.TsMeta;
import com.ambitious.v2.util.LogUtils;
import com.ambitious.v2.util.SleepUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Deque;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * m3u8 多线程下载执行期
 * @author ambitious
 * @date 2023/5/9
 */
public class M3U8MultiThreadActuator extends M3U8Actuator {

    private static final Logger LOGGER = LoggerFactory.getLogger(M3U8MultiThreadActuator.class);

    @Override
    protected void handleDownload(DownloadMeta meta, Deque<TsMeta> tsMetas, File tempDir) {
        AtomicInteger finish = new AtomicInteger(0);
        int size = tsMetas.size();
        while (finish.get() < size) {
            if (tsMetas.isEmpty()) {
                // 可能会有下载失败的 ts 文件，先阻塞两秒
                SleepUtils.sleep(2000);
                continue;
            }
            TsMeta tsMeta = tsMetas.pollFirst();
            DownloadThreadPool.submit(() -> {
                try {
                    coreDownload(meta, tsMeta, tempDir);
                    LogUtils.info(LOGGER, String.format("请求 ts 文件中，当前进度：%d / %d，目标视频：%s", finish.incrementAndGet(), size, meta.getFileName()));
                } catch (Exception e) {
                    LogUtils.error(LOGGER, String.format("请求失败：%s，重新加入到队列中，目标视频：%s", e.getMessage(), meta.getFileName()));
                    tsMetas.offerLast(tsMeta);
                }
            });
        }
        LogUtils.success(LOGGER, "ts 列表下载完成");
    }
}
