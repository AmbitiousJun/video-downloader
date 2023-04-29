package com.ambitious.core.downloader.hutool;

import cn.hutool.core.io.StreamProgress;
import cn.hutool.http.HttpUtil;
import com.ambitious.core.downloader.UrlDownloader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author ambitious
 * @date 2023/4/29
 */
public class HuToolUrlDownloader implements UrlDownloader {

    private final Logger logger = LoggerFactory.getLogger(HuToolUrlDownloader.class);

    @Override
    public void downloadWithoutProgress(String url, File dest) {
        HttpUtil.downloadFile(url, dest);
    }

    @Override
    @SuppressWarnings("all")
    public void downloadWithProgress(String url, File dest) {
        // 这个值为 true 的话就在控制台输出下载进度
        AtomicBoolean progressTipFlag = new AtomicBoolean(false);
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                progressTipFlag.set(true);
            }
        }, 0, 10000);
        String fileName = dest.getName();
        HttpUtil.downloadFile(url, dest, new StreamProgress() {

            @Override
            public void start() {
                logger.info("开始下载，文件名：{}", fileName);
            }

            @Override
            public void progress(long total, long progressSize) {
                if (!progressTipFlag.get()) {
                    return;
                }
                if (total == -1) {
                    logger.info("下载进度：{}，文件名：{}", progressSize, fileName);
                } else {
                    BigDecimal tt = new BigDecimal(total);
                    BigDecimal ps = new BigDecimal(progressSize);
                    BigDecimal percent = ps.divide(tt, new MathContext(4, RoundingMode.HALF_UP)).multiply(new BigDecimal(100));
                    logger.info("下载进度：{}%，文件名：{}", percent, fileName);
                }
                progressTipFlag.set(false);
            }

            @Override
            public void finish() {
                logger.info("下载完成，文件名：{}", fileName);
                timer.cancel();
            }
        });
    }
}
