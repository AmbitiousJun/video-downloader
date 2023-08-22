package com.ambitious.test;

import com.ambitious.v2.downloader.actuator.DownloadActuator;
import com.ambitious.v2.downloader.actuator.M3U8MultiThreadActuator;
import com.ambitious.v2.pojo.DownloadMeta;
import com.ambitious.v2.pojo.TsMeta;
import com.ambitious.v2.util.M3U8Utils;
import com.google.common.collect.Maps;

import java.util.Deque;

/**
 * @author ambitious
 * @date 2023/6/7
 */
public class App5 {

    public static void main(String[] args) throws Exception {
        String url = "file:///Users/ambitious/临时文件/古宅风云1.m3u8";
        DownloadMeta meta = new DownloadMeta(url, "/Users/ambitious/downloads/2020-08-06 古宅风云上.mp4", "");
        DownloadActuator actuator = new M3U8MultiThreadActuator();
        actuator.download(meta);
    }
}
