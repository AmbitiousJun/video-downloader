package com.ambitious.v2.decoder.selenium;

import com.ambitious.v2.config.Config;
import com.ambitious.v2.util.LogUtils;
import net.lightbody.bmp.BrowserMobProxy;
import net.lightbody.bmp.BrowserMobProxyServer;
import net.lightbody.bmp.client.ClientUtil;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.CapabilityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;

/**
 * 使用 Selenium 实现视频解析
 * @author ambitious
 * @date 2023/5/6
 */
public abstract class SeleniumDecoder implements Closeable {

    /**
     * Chrome 浏览器驱动
     */
    protected ChromeDriver driver;
    /**
     * 浏览器代理插件
     */
    protected BrowserMobProxy proxy;

    protected final Logger logger = LoggerFactory.getLogger(SeleniumDecoder.class);

    public SeleniumDecoder(PageLoadStrategy strategy, boolean useProxy) {
        this.init(strategy, useProxy);
    }

    /**
     * 从网页中解析出下载链接
     * @param fetchUrl 要解析的网页
     * @return 下载链接
     */
    public abstract String fetchDownloadLink(String fetchUrl);

    @Override
    public void close() throws IOException {
        try {
            if (this.proxy != null) {
                this.proxy.stop();
                this.proxy = null;
            }
            if (this.driver != null) {
                this.driver.quit();
                this.driver = null;
            }
        } catch (Exception e) {
            LogUtils.warning(logger, "ChromeDriver 关闭异常：" + e.getMessage());
        }
    }

    protected String formatLogSuffix(String fetchUrl) {
        return " --- 源地址: " + fetchUrl;
    }

    /**
     * 初始化浏览器
     * @param strategy 浏览器页面加载策略
     * @param useProxy 是否使用代理
     */
    private void init(PageLoadStrategy strategy, boolean useProxy) {
        try {
            LogUtils.info(logger, "正在初始化 Chrome 客户端...");
            // 配置Chrome浏览器
            ChromeOptions options = new ChromeOptions();
            options.setCapability(CapabilityType.ACCEPT_SSL_CERTS, true);
            options.setPageLoadStrategy(strategy);
            options.setAcceptInsecureCerts(true);
            if (!Config.SELENIUM.SHOW_WINDOW) {
                // 不开启 GUI 界面，在后台运行浏览器
                options.addArguments("--headless");
            }
            if (useProxy) {
                // 开启代理
                this.proxy = new BrowserMobProxyServer();
                proxy.start(0, InetAddress.getLocalHost());
                Proxy seleniumProxy = ClientUtil.createSeleniumProxy(proxy);
                options.setCapability(CapabilityType.PROXY, seleniumProxy);
            }
            // 设置ChromeDriver路径
            System.setProperty("webdriver.chrome.driver", Config.SELENIUM.CHROME_DRIVER_PATH);
            this.driver = new ChromeDriver(options);
            LogUtils.success(logger, "Chrome 客户端初始化成功");
        } catch (Exception e) {
            LogUtils.error(logger, "Chrome 客户端初始化失败：" + e.getMessage());
            System.exit(-1);
        }
    }
}
