package com.ambitious.v2.decoder.selenium;

import cn.hutool.core.util.IdUtil;
import com.ambitious.v2.config.Config;
import net.lightbody.bmp.core.har.Har;
import net.lightbody.bmp.core.har.HarEntry;
import org.openqa.selenium.PageLoadStrategy;

import java.util.List;

/**
 * @author ambitious
 * @date 2023/5/6
 */
public class FreeApiSeleniumDecoder extends SeleniumDecoder{

    public FreeApiSeleniumDecoder() {
        super(PageLoadStrategy.NORMAL, true);
    }

    @Override
    public String fetchDownloadLink(String fetchUrl) {
        logger.info("正在获取视频的真实下载链接...{}", formatLogSuffix(fetchUrl));
        // 1 判断客户端是否初始化
        if (super.proxy == null || super.driver == null) {
            throw new RuntimeException("Chrome 客户端未初始化," + formatLogSuffix(fetchUrl));
        }
        // 2 访问网页
        super.proxy.newHar(IdUtil.randomUUID());
        super.driver.get(fetchUrl);
        logger.info("等待解析完成... {} 秒,{}", Config.DECODER.FREE_API.WAIT_SECONDS, formatLogSuffix(fetchUrl));
        try {
            Thread.sleep(Config.DECODER.FREE_API.WAIT_SECONDS * 1000);
        } catch (InterruptedException e) {
            throw new RuntimeException("解析被中断," + formatLogSuffix(fetchUrl));
        }
        logger.info("尝试获取视频下载链接...,{}", formatLogSuffix(fetchUrl));
        // 3 分析抓包文件，提取下载链接
        Har har = super.proxy.getHar();
        List<HarEntry> entries = har.getLog().getEntries();
        for (HarEntry entry : entries) {
            String possibleUrl = entry.getRequest().getUrl();
            for (String validPrefix : Config.DECODER.FREE_API.VALID_URL_PREFIXES) {
                if (possibleUrl.contains(validPrefix)) {
                    logger.info("成功获取到下载链接：{},{}", possibleUrl, formatLogSuffix(fetchUrl));
                    return possibleUrl;
                }
            }
        }
        throw new RuntimeException("获取不到下载链接," + formatLogSuffix(fetchUrl));
    }
}
