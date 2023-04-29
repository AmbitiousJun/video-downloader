package com.ambitious.core;

import cn.hutool.core.util.IdUtil;
import com.google.common.collect.Sets;
import net.lightbody.bmp.BrowserMobProxy;
import net.lightbody.bmp.BrowserMobProxyServer;
import net.lightbody.bmp.client.ClientUtil;
import net.lightbody.bmp.core.har.Har;
import net.lightbody.bmp.core.har.HarEntry;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 自己封装的 Selenium 类
 * @author ambitious
 * @date 2023/4/28
 */
public class MySelenium {

    /**
     * Chrome 驱动路径
     */
    private static final String DRIVER_PATH = "/Users/ambitious/App/chromedriver_mac_arm64/chromedriver";
    private BrowserMobProxy proxy;
    private WebDriver driver;
    private final Set<String> validUrlPrefix = Sets.newHashSet(
        "https://om.tc.qq.com",
            "https://sluiceyf.titan.mgtv.com"
    );
    private final int retryTimes = 6;
    private Logger logger = LoggerFactory.getLogger(MySelenium.class);

    public MySelenium() {
        // 初始化客户端
        this.initClient();
    }

    /**
     * 使用 Chrome 客户端访问地址 url，获取视频的真实下载地址
     * @param url 要访问的地址
     * @param waitSeconds 进入网页后程序阻塞的秒数
     * @return 视频的真实下载地址
     */
    public synchronized String fetchDownloadLink(String url, int waitSeconds) {
        logger.info("正在获取视频的真实下载链接...");
        // 1 判断客户端是否初始化
        if (this.proxy == null || this.driver == null) {
            logger.error("Chrome 客户端未初始化");
            this.destroy();
            System.exit(-1);
        }
        // 2 开启代理，访问网页
        int times = 1;
        while (times <= retryTimes) {
            this.proxy.newHar(IdUtil.randomUUID());
            this.driver.get(url);
            logger.info("等待解析完成... {} 秒，第 {} 次尝试", waitSeconds ,times);
            try {
                Thread.sleep(waitSeconds * 1000);
            } catch (InterruptedException e) {
                logger.error("程序运行中断");
                System.exit(-1);
            }
            logger.info("等待结束，开始获取视频下载链接...");
            // 3 分析抓包文件，提取下载链接
            Har har = this.proxy.getHar();
            List<HarEntry> entries = har.getLog().getEntries();
            for (HarEntry entry : entries) {
                String possibleUrl = entry.getRequest().getUrl();
                for (String validPrefix : validUrlPrefix) {
                    if (possibleUrl.contains(validPrefix)) {
                        logger.info("成功获取到下载链接：{}", possibleUrl);
                        return possibleUrl;
                    }
                }
            }
            times++;
        }
        throw new RuntimeException("获取不到下载链接");
    }

    /**
     * 关闭客户端
     */
    public void destroy() {
        logger.info("正在关闭 Chrome 客户端...");
        if (this.proxy != null) {
            this.proxy.stop();
            this.proxy = null;
        }
        if (this.driver != null) {
            this.driver.close();
            this.driver = null;
        }
        logger.info("Chrome 客户端已关闭");
    }

    /**
     * 初始化 Chrome 客户端
     */
    public void initClient() {
        try {
            logger.info("正在初始化 Chrome 客户端...");
            // 启动BrowserMob Proxy
            this.proxy = new BrowserMobProxyServer();
            proxy.start(0, InetAddress.getLocalHost());

            // 配置Selenium WebDriver使用代理
            Proxy seleniumProxy = ClientUtil.createSeleniumProxy(proxy);

            // 配置Chrome浏览器
            DesiredCapabilities capabilities = new DesiredCapabilities();
            capabilities.setCapability(CapabilityType.PROXY, seleniumProxy);
            capabilities.setCapability(CapabilityType.ACCEPT_SSL_CERTS, true);

            ChromeOptions options = new ChromeOptions();
            options.merge(capabilities);
            options.setCapability(CapabilityType.ACCEPT_SSL_CERTS, true);
            options.setAcceptInsecureCerts(true);
            // 不开启 GUI 界面，在后台运行程序
            options.addArguments("--headless");

            // 设置ChromeDriver路径
            System.setProperty("webdriver.chrome.driver", DRIVER_PATH);
            // SSL 握手超时时间为 1 分钟
            // System.setProperty("javax.net.ssl.handshakeTimeout", "60000");

            this.driver = new ChromeDriver(options);
            logger.info("Chrome 客户端初始化成功");
        } catch (Exception e) {
            logger.error("Chrome 客户端初始化失败", e);
            System.exit(-1);
        }
    }

}
