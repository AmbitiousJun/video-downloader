package com.ambitious.v1.downloader;

import java.io.File;

/**
 * 通用的 URL 文件下载器接口
 * @author ambitious
 * @date 2023/4/29
 */
public interface UrlDownloader {

    /**
     * 下载文件，不输出下载进度
     * @param url 下载地址
     * @param dest 本地保存地址
     * @throws Exception 文件下载失败
     */
    void downloadWithoutProgress(String url, File dest) throws Exception;

    /**
     * 下载文件，同时输出下载进度
     * @param url 下载地址
     * @param dest 本地保存地址
     * @throws Exception 文件下载失败
     */
    void downloadWithProgress(String url, File dest) throws Exception;
}
