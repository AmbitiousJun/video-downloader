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

/**
 * 下载器配置
 * @author ambitious
 * @date 2023/5/4
 */
@Data
@SuppressWarnings("all")
public class DownloaderConfig {

    public static final Logger LOGGER = LoggerFactory.getLogger(DownloaderConfig.class);
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
        // 默认速率是 5mbps
        int rate = 5 * 1024 * 1024;
        final String kbps = "kbps";
        final String mbps = "mbps";
        if (StrUtil.isNotEmpty(rateLimit)) {
            if (rateLimit.endsWith(kbps)) {
                rate = Integer.parseInt(rateLimit.substring(0, rateLimit.length() - kbps.length()));
                LogUtils.success(LOGGER, "下载器速率限制：" + rate + kbps);
                rate *= 1024;
            }
            if (rateLimit.endsWith(mbps)) {
                rate = Integer.parseInt(rateLimit.substring(0, rateLimit.length() - mbps.length()));
                LogUtils.success(LOGGER, "下载器速率限制：" + rate + mbps);
                rate *= 1024 * 1024;
            }
        }
        this.TOKEN_BUCKET = new MyTokenBucket(rate);
    }
}
