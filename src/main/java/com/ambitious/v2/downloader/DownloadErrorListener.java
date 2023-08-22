package com.ambitious.v2.downloader;

import com.ambitious.v2.pojo.DownloadMeta;

/**
 * 下载失败处理器，用于将任务重新加入到监听列表中
 * @author ambitious
 * @date 2023/6/14
 */
@FunctionalInterface
public interface DownloadErrorListener {

    /**
     * 处理方法
     * @param meta 下载失败的数据信息
     */
    void emit(DownloadMeta meta);
}
