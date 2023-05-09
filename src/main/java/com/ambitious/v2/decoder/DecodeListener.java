package com.ambitious.v2.decoder;

import com.ambitious.v2.pojo.DownloadMeta;

/**
 * @author ambitious
 * @date 2023/5/7
 */
@FunctionalInterface
public interface DecodeListener {

    /**
     * 每当成功解析出一条下载链接的时候，这个方法就会被调用一次
     * @param meta 带下载链接的下载数据
     */
    void newDecodeUrl(DownloadMeta meta);
}
