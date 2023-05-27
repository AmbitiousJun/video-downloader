package com.ambitious.v2.downloader.actuator;

import cn.hutool.core.io.StreamProgress;
import cn.hutool.http.HttpUtil;
import com.ambitious.v2.pojo.DownloadMeta;
import com.ambitious.v2.util.LogUtils;
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
 * 单线程下载 MP4 文件
 * @author ambitious
 * @date 2023/5/8
 */
public class Mp4SimpleActuator implements DownloadActuator{
    
    private static final Logger LOGGER = LoggerFactory.getLogger(Mp4SimpleActuator.class);

    @Override
    @SuppressWarnings("all")
    public void download(DownloadMeta meta) throws Exception {
        // 这个值为 true 的话就在控制台输出下载进度
        AtomicBoolean progressTipFlag = new AtomicBoolean(false);
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                progressTipFlag.set(true);
            }
        }, 0, 10000);
        File dest = new File(meta.getFileName());
        String fileName = dest.getName();
        HttpUtil.downloadFile(meta.getLink(), dest, new StreamProgress() {

            @Override
            public void start() {
                LogUtils.info(LOGGER, String.format("开始下载，文件名：%d", fileName));
            }

            @Override
            public void progress(long total, long progressSize) {
                if (!progressTipFlag.get()) {
                    return;
                }
                if (total == -1) {
                    LogUtils.info(LOGGER, String.format("下载进度：%s，文件名：%s", progressSize, fileName));
                } else {
                    BigDecimal tt = new BigDecimal(total);
                    BigDecimal ps = new BigDecimal(progressSize);
                    BigDecimal percent = ps.divide(tt, new MathContext(4, RoundingMode.HALF_UP)).multiply(new BigDecimal(100));
                    LogUtils.info(LOGGER, String.format("下载进度：{}%，文件名：{}", percent, fileName));
                }
                progressTipFlag.set(false);
            }

            @Override
            public void finish() {
                LogUtils.success(LOGGER, String.format("下载完成，文件名：%s", fileName));
                timer.cancel();
            }
        });
    }
}
