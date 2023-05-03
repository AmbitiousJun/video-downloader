package com.ambitious.core.downloader.m3u8.transfer;

import java.io.File;

/**
 * ts 文件转换器
 * @author ambitious
 * @date 2023/5/3
 */
public interface TsTransfer {

    /**
     * 将 ts 格式的文件列表转换成 mp4 格式的视频文件
     * @param tsDir 存放 ts 文件的目录
     * @param output 合并后输出的文件
     * @throws Exception
     */
    void ts2Mp4(File tsDir, File output) throws Exception;
}
