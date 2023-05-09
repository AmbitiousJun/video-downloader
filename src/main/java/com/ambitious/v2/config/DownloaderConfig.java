package com.ambitious.v2.config;

import cn.hutool.core.util.StrUtil;
import com.ambitious.v2.constant.DownloaderType;
import lombok.Data;

/**
 * 下载器配置
 * @author ambitious
 * @date 2023/5/4
 */
@Data
public class DownloaderConfig {

    public final DownloaderType USE;
    public final Integer TASK_THREAD_COUNT;
    public final Integer DL_THREAD_COUNT;
    public final String DOWNLOAD_DIR;
    public final String TS_DIR_SUFFIX;

    public DownloaderConfig(DownloaderType use, int taskThreadCount, int dlThreadCount, String downloadDir, String tsDirSuffix) {
        if (StrUtil.isEmpty(downloadDir)) {
            throw new RuntimeException("下载文件夹目录为空");
        }
        this.DOWNLOAD_DIR = downloadDir;
        this.USE = use == null ? DownloaderType.MULTI_THREAD : use;
        this.TASK_THREAD_COUNT = taskThreadCount > 0 ? taskThreadCount : 2;
        this.DL_THREAD_COUNT = dlThreadCount > 0 ? dlThreadCount : 32;
        this.TS_DIR_SUFFIX = StrUtil.isNotEmpty(tsDirSuffix) ? tsDirSuffix : "temp_ts_files";
    }
}
