package com.ambitious.v2.config;

import cn.hutool.core.util.StrUtil;
import com.ambitious.v2.constant.DownloaderType;
import com.ambitious.v2.util.LogUtils;
import com.ambitious.v2.util.MyTokenBucket;
import com.google.common.util.concurrent.RateLimiter;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

/**
 * 下载器配置
 * @author ambitious
 * @date 2023/5/4
 */
@Data
@SuppressWarnings("all")
public class DownloaderConfig {

    public static final Logger LOGGER = LoggerFactory.getLogger(DownloaderConfig.class);
    public static final Double RATE_LIMIT_MAX_VALUE_KBPS = Math.floor(1.0 * Integer.MAX_VALUE / 2 / 1024);
    public static final Double RATE_LIMIT_MIN_VALUE_KBPS = 1.0 * 10;
    public static final Double RATE_LIMIT_MAX_VALUE_MBPS = Math.floor(RATE_LIMIT_MAX_VALUE_KBPS / 1024);
    public static final Double RATE_LIMIT_MIN_VALUE_MBPS = 0.1;
    public final DownloaderType USE;
    public final Integer TASK_THREAD_COUNT;
    public final Integer DL_THREAD_COUNT;
    public final String DOWNLOAD_DIR;
    public final String TS_DIR_SUFFIX;
    public final MyTokenBucket TOKEN_BUCKET;

    public DownloaderConfig(DownloaderType use, int taskThreadCount, int dlThreadCount, String downloadDir, String tsDirSuffix, String rateLimit) {
        if (StrUtil.isEmpty(downloadDir)) {
            throw new RuntimeException("下载文件夹目录为空");
        }
        this.DOWNLOAD_DIR = downloadDir;
        this.USE = use == null ? DownloaderType.MULTI_THREAD : use;
        this.TASK_THREAD_COUNT = taskThreadCount > 0 ? taskThreadCount : 2;
        this.DL_THREAD_COUNT = dlThreadCount > 0 ? dlThreadCount : 32;
        this.TS_DIR_SUFFIX = StrUtil.isNotEmpty(tsDirSuffix) ? tsDirSuffix : "temp_ts_files";
        try {
            // 默认速率是 5mbps
            double rate = 5 * 1024 * 1024;
            final String kbps = "kbps";
            final String mbps = "mbps";
            if (StrUtil.isNotEmpty(rateLimit)) {
                if (rateLimit.equals("-1")) {
                    rate = RATE_LIMIT_MAX_VALUE_MBPS * 1024 * 1024;
                    LogUtils.success(LOGGER, "下载器不开启速率限制");
                } else if (rateLimit.endsWith(kbps)) {
                    String val = rateLimit.substring(0, rateLimit.length() - kbps.length());
                    rate = checkKbpsRateLimit(val);
                    LogUtils.success(LOGGER, "下载器速率限制：" + rate + kbps);
                    rate *= 1024;
                } else if (rateLimit.endsWith(mbps)) {
                    String val = rateLimit.substring(0, rateLimit.length() - mbps.length());
                    rate = checkMbpsRateLimit(val);
                    LogUtils.success(LOGGER, "下载器速率限制：" + rate + mbps);
                    rate *= 1024 * 1024;
                } else {
                    LogUtils.warning(LOGGER, "没有配置限速或者配置出错，启用默认的速率限制：5mbps");
                }
            }
            this.TOKEN_BUCKET = new MyTokenBucket(new BigDecimal(rate).intValue());
        } catch (Exception e) {
            throw new IllegalArgumentException("速率限制配置不规范，" + e.getMessage());
        }
    }

    private double checkMbpsRateLimit(String val) {
        double res = Double.parseDouble(val);
        if (res < RATE_LIMIT_MIN_VALUE_MBPS || res > RATE_LIMIT_MAX_VALUE_MBPS) {
            throw new IllegalArgumentException(String.format("速率限制范围（mbps）：[%s, %s]", RATE_LIMIT_MIN_VALUE_MBPS, RATE_LIMIT_MAX_VALUE_MBPS));
        }
        return res;
    }

    private double checkKbpsRateLimit(String val) {
        double res = Double.parseDouble(val);
        if (res < RATE_LIMIT_MIN_VALUE_KBPS || res > RATE_LIMIT_MAX_VALUE_KBPS) {
            throw new IllegalArgumentException(String.format("速率限制范围（kbps）：[%s, %s]", RATE_LIMIT_MIN_VALUE_KBPS, RATE_LIMIT_MAX_VALUE_KBPS));
        }
        return res;
    }
}
