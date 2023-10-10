package com.ambitious.v2.downloader.actuator.mp4multithread;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 将每个分片的下载区间保存下来
 * @author ambitious
 * @date 2023/4/29
 */
@Data
@AllArgsConstructor
public class UnitTask {

    /**
     * 起始字节
     */
    private long from;
    /**
     * 结束字节
     */
    private long to;
}
