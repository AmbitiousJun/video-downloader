package com.ambitious.v2.downloader.actuator;

import com.ambitious.v2.pojo.DownloadMeta;

/**
 * 执行器
 * @author ambitious
 * @date 2023/5/8
 */
public interface DownloadActuator {

    /**
     * 执行下载
     * @param meta 下载信息
     * @throws Exception 异常
     */
    void download(DownloadMeta meta) throws Exception;
}
