package com.ambitious.v2.decoder.selenium;

import cn.hutool.core.util.IdUtil;
import com.ambitious.v2.config.Config;
import com.ambitious.v2.constant.MediaType;
import com.ambitious.v2.constant.VideoSite;
import com.ambitious.v2.util.FileUtils;
import com.google.common.collect.Lists;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.PageLoadStrategy;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author ambitious
 * @date 2023/5/6
 */
public class VipFetchSeleniumDecoder extends SeleniumDecoder{

    /**
     * 用于初始化网站相关数据：Cookie、清晰度
     */
    private volatile SiteInitializer siteInitializer;

    public VipFetchSeleniumDecoder() {
        super(PageLoadStrategy.EAGER, false);
    }

    @Override
    public String fetchDownloadLink(String fetchUrl) {
        try {
            logger.info("检查 Chrome 客户端是否初始化...,{}", formatLogSuffix(fetchUrl));
            if (super.driver == null) {
                throw new RuntimeException("Chrome 客户端未初始化");
            }
            logger.info("Chrome 客户端初始化完成,{}", formatLogSuffix(fetchUrl));
            logger.info("选择视频网站初始化器...,{}", formatLogSuffix(fetchUrl));
            // 选择一个网站初始化器
            selectSiteInitializer();
            logger.info("视频网站初始化器选择完成...,{}", formatLogSuffix(fetchUrl));
            logger.info("正在初始化视频网站...,{}", formatLogSuffix(fetchUrl));
            this.siteInitializer.init(fetchUrl);
            logger.info("视频网站初始化完成,{}", formatLogSuffix(fetchUrl));
            logger.info("正在将资源嗅探脚本注入到网页中...,{}", formatLogSuffix(fetchUrl));
            // 注入资源嗅探脚本
            injectMediaFetchScript();
            logger.info("脚本注入完成...,{}", formatLogSuffix(fetchUrl));
            if (!driver.getCurrentUrl().startsWith("blob:")) {
                throw new RuntimeException("资源嗅探失败");
            }
            String m3u8 = driver.getPageSource();
            String prefix = "#EXTM3U";
            String suffix = "#EXT-X-ENDLIST";
            int pIdx = m3u8.indexOf(prefix);
            int sIdx = m3u8.indexOf(suffix);
            if (pIdx == -1 || sIdx == -1) {
                throw new RuntimeException("资源嗅探失败");
            }
            logger.info("资源嗅探成功，正在将 M3U8 文件保存到本地...,{}", formatLogSuffix(fetchUrl));
            m3u8 = m3u8.substring(pIdx, sIdx + suffix.length());
            String path = Config.DOWNLOADER.DOWNLOAD_DIR + "/" + MediaType.M3U8;
            String filename = IdUtil.randomUUID() + "." + MediaType.M3U8;
            String finalPath = path + "/" + filename;
            FileUtils.initFileDirs(finalPath);
            Files.write(Paths.get(finalPath), m3u8.getBytes(StandardCharsets.UTF_8));
            logger.info("M3U8 文件已保存，路径：{},{}", finalPath, formatLogSuffix(fetchUrl));
            return "file://" + finalPath;
        } catch (Exception e) {
            throw new RuntimeException("获取视频下载链接失败", e);
        }
    }

    /**
     * 注入猫抓资源嗅探脚本
     * @throws Exception 脚本注入失败
     */
    private void injectMediaFetchScript() throws Exception {
        URL scriptUrl = this.getClass().getClassLoader().getResource("media-fetch.js");
        if (scriptUrl == null) {
            throw new RuntimeException("找不到脚本文件 media-fetch.js");
        }
        String script = new String(Files.readAllBytes(Paths.get(scriptUrl.toURI())));
        // 刷新页面之后立马注入脚本，这里有个前提条件是页面加载策略为 Eager
        driver.navigate().refresh();
        driver.executeScript(script);
        // 等待嗅探完成
        Thread.sleep(20000);
    }

    /**
     * 根据用户配置的解析网站，选择相应的网站初始化器
     */
    private void selectSiteInitializer() {
        if (this.siteInitializer == null) {
            synchronized (this) {
                if (this.siteInitializer == null) {
                    String use = Config.DECODER.VIP_FETCH.USE;
                    if (VideoSite.QY.equals(use)) {
                        logger.info("选择爱奇艺初始化器");
                        this.siteInitializer = new QyInitializer(driver, getCookies());
                    } else {
                        // TODO 等到其他的初始化器实现之后，再来补全这段逻辑
                        throw new RuntimeException("暂不支持解析 " + use + " 中的视频");
                    }
                }
            }
            return;
        }
        logger.info("初始化器已存在");
    }

    private List<Cookie> getCookies() {
        Map<String, String> cookieMap = Config.DECODER.VIP_FETCH.COOKIES;
        List<Cookie> res = Lists.newArrayList();
        for (String key : cookieMap.keySet()) {
            res.add(
                new Cookie(
                    key,
                    cookieMap.get(key),
                    Config.DECODER.VIP_FETCH.DOMAIN,
                    "/",
                    new Date(System.currentTimeMillis() + 1000 * 3600 * 24 * 7)
                )
            );
        }
        return res;
    }
}
