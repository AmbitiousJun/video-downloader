package com.ambitious.test;

import net.lightbody.bmp.BrowserMobProxy;
import net.lightbody.bmp.BrowserMobProxyServer;
import net.lightbody.bmp.client.ClientUtil;
import net.lightbody.bmp.core.har.Har;
import net.lightbody.bmp.core.har.HarEntry;
import net.lightbody.bmp.core.har.HarRequest;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;

/**
 * 测试抓包
 *
 * @author ambitious
 */
public class App6 {

    public static void main( String[] args ) throws IOException, InterruptedException {
        String video = "http://tv.haoqu99.com/1/cctv1.html";

        // 启动BrowserMob Proxy
        BrowserMobProxy proxy = new BrowserMobProxyServer();
        proxy.setTrustAllServers(true);
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
        // options.addArguments("--headless");

        // 设置ChromeDriver路径
        System.setProperty("webdriver.chrome.driver", "/Users/ambitious/App/chromedriver_mac_arm64/chromedriver");

        WebDriver driver = new ChromeDriver(options);

        // 启用代理服务器抓取请求
        proxy.newHar("test");
        // proxy.enableHarCaptureTypes(CaptureType.REQUEST_CONTENT, CaptureType.RESPONSE_CONTENT);

        // 导航到网页
        driver.get(video);

        // 等待视频加载完成
        Thread.sleep(15000);

        Har har = proxy.getHar();
        // File harFile = new File("test.har");
        // har.writeTo(harFile);
        List<HarEntry> entries = har.getLog().getEntries();
        for (HarEntry entry : entries) {
            HarRequest request = entry.getRequest();
            System.out.println(request.getUrl());
        }

        proxy.stop();
        driver.close();

    }

    private static String findVideoUrl(String har) {
        // 在HAR文件中查找包含.m3u8或.ts的URL
        // 然后从URL中提取出视频地址
        String[] lines = har.split("\n");
        for (String line : lines) {
            if (line.contains(".m3u8") || line.contains(".ts")) {
                String[] parts = line.split("\"");
                for (String part : parts) {
                    if (part.contains(".m3u8") || part.contains(".ts")) {
                        return part;
                    }
                }
            }
        }
        return null;
    }
}
