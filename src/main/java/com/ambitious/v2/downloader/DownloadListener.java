package com.ambitious.v2.downloader;

/**
 * 任务下载监听器
 * @author ambitious
 * @date 2023/5/8
 */
@FunctionalInterface
public interface DownloadListener {

    /**
     * 下载完成回调
     */
    void completeOne();
}
