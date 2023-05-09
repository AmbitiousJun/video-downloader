package com.ambitious.v2.decoder.selenium;

import org.openqa.selenium.chrome.ChromeDriver;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * 用于初始化 Selenium 相应站点的 Cookie
 * @author ambitious
 * @date 2023/5/6
 */
public interface SiteInitializer {

    /**
     * 调用这个方法统一初始化视频网站 Cookie
     * @param url 相应网站的任意视频播放地址，主要是为了设置视频的清晰度
     */
    void init(String url);


}
