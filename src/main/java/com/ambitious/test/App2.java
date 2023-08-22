package com.ambitious.test;

import com.ambitious.v2.config.Config;
import com.google.common.collect.Lists;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.remote.CapabilityType;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 测试通过 Selenium 注入 Cookie 实现自动登录，注入 JS 拦截请求
 * @author ambitious
 * @date 2023/4/28
 */
public class App2 {

    public static void main(String[] args) throws InterruptedException, IOException {
        ChromeDriver driver = null;
        try {
            // 配置Chrome浏览器
            ChromeOptions options = new ChromeOptions();
            options.setCapability(CapabilityType.ACCEPT_SSL_CERTS, true);
            options.setAcceptInsecureCerts(true);
            options.setPageLoadStrategy(PageLoadStrategy.EAGER);
            // options.addArguments("--headless");

            // 设置ChromeDriver路径
            System.setProperty("webdriver.chrome.driver", "/Users/ambitious/App/chromedriver_mac_arm64/chromedriver");

            driver = new ChromeDriver(options);

            final String url = "https://v.qq.com";

            driver.get(url);

            // 设置 Cookie
            List<Cookie> cookies = getTxCookies();
            for (Cookie cookie : cookies) {
                driver.manage().addCookie(cookie);
            }

            // 重新加载网页，让 Cookie 生效
            driver.navigate().refresh();
            Thread.sleep(10000);
            driver.navigate().refresh();

            if (1 == 0) {
                // 将分辨率切换至 1080p
                Thread.sleep(10000);
                // 弹出清晰度选择框
                WebElement panel = driver.findElement(By.cssSelector("iqpdiv[data-player-hook='definitionPanel']"));
                String script = "arguments[0].setAttribute('style', 'display: block')";
                driver.executeScript(script, panel);
                // 获取 1080P 按钮并点击
                Actions actions = new Actions(driver);
                WebElement btn = driver.findElement(By.cssSelector("iqp[class='iqp-txt-stream'][data-player-hook='5']"));
                actions.moveToElement(btn).click().perform();

                // 等待清晰度切换完成
                Thread.sleep(20000);

                // 注入资源嗅探脚本
                script = readScript(new File("media-fetch.js"));
                driver.executeScript("location.href = '" + url + "'");
                driver.executeScript(script);

                // 等待 10 秒之后判断是否嗅探成功
                Thread.sleep(10000);
                if (!driver.getCurrentUrl().startsWith("blob:")) {
                    throw new RuntimeException("资源嗅探失败");
                }

                // 将 m3u8 文件保存到本地
                String m3u8 = driver.getPageSource();
                String prefix = "#EXTM3U";
                String suffix = "#EXT-X-ENDLIST";
                int pIdx = m3u8.indexOf(prefix);
                int sIdx = m3u8.indexOf(suffix);
                if (pIdx == -1 || sIdx == -1) {
                    throw new RuntimeException("资源嗅探失败");
                }
                m3u8 = m3u8.substring(pIdx, sIdx + suffix.length());
                Files.write(Paths.get("/Users/ambitious/Downloads/测试.m3u8"), m3u8.getBytes(StandardCharsets.UTF_8));
            }

        } finally {
            if (driver != null) {
                driver.close();
                driver.quit();
            }
        }
    }

    private static List<Cookie> getTxCookies() {
        final String domain = ".v.qq.com";
        Map<String, String> cookies = Config.DECODER.VIP_FETCH.COOKIES;
        List<Cookie> res = Lists.newArrayList();
        for (String key : cookies.keySet()) {
            res.add(new Cookie(key, cookies.get(key), domain, "/", new Date(System.currentTimeMillis() + 1000 * 3600 * 24 * 7)));
        }
        return res;
    }

    private static List<Cookie> getQiYiCookies() {
        final String domain = ".iqiyi.com";
        List<Cookie> res = Lists.newArrayList();
        res.add(new Cookie("P00001", "91dN79c9qObgCE2vfwvm1OGl91NBzlUVfSxSLW5Tk2uM1Rcom28b4FazZV38GjpLA9EM10", domain, "/", new Date(System.currentTimeMillis() + 1000 * 3600 * 24 * 7)));
        res.add(new Cookie("P00002", "%7B%22uid%22%3A%221764530833%22%2C%22pru%22%3A1764530833%2C%22user_name%22%3A%22180****8823%22%2C%22nickname%22%3A%22%5Cu7528%5Cu6237692c9a91%22%2C%22pnickname%22%3A%22%5Cu7528%5Cu6237692c9a91%22%2C%22type%22%3A11%2C%22email%22%3Anull%7D", domain, "/", new Date(System.currentTimeMillis() + 1000 * 3600 * 24 * 7)));
        res.add(new Cookie("P00003", "1764530833", domain, "/", new Date(System.currentTimeMillis() + 1000 * 3600 * 24 * 7)));
        res.add(new Cookie("P00004", ".1683175241.6be1a84d5e", domain, "/", new Date(System.currentTimeMillis() + 1000 * 3600 * 24 * 7)));
        res.add(new Cookie("P00007", "91dN79c9qObgCE2vfwvm1OGl91NBzlUVfSxSLW5Tk2uM1Rcom28b4FazZV38GjpLA9EM10", domain, "/", new Date(System.currentTimeMillis() + 1000 * 3600 * 24 * 7)));
        res.add(new Cookie("P00037", "A00000", domain, "/", new Date(System.currentTimeMillis() + 1000 * 3600 * 24 * 7)));
        res.add(new Cookie("P00040", "\"CmBRRVd5RXI5VFRwNUtLV3A2TXhtVnJVaCUyQkZERjlRYVRkM0U2UjYyeGNUckIzUEpDZTdoTEFwTVhaZEJXd3ZTbEplbXFzNFpQYUVEUG0lMkJ6RWU3d1hTUmclM0QlM0QQASoBMTAAUB1gAGogMTQ3MzExZWU0MjM2NTdmNmMyY2QwZTJmM2EyNGY3NmGCAUJodHRwOi8vcGFzc3BvcnQuaXFpeWkuY29tL2FwaXMvdGhpcmRwYXJ0eS9uY2FsbGJhY2suYWN0aW9uP2Zyb209MjmIAQGQAQDIAQHaARQwMTAxMDAyMTAxMDAwMDAwMDAwMJoCK2h0dHBzOi8vd3d3LmlxaXlpLmNvbS90aGlyZGxvZ2luL2Nsb3NlLmh0bWyiAitodHRwczovL3d3dy5pcWl5aS5jb20vdGhpcmRsb2dpbi9jbG9zZS5odG1sqgIBMboCATE=\"", domain, "/", new Date(System.currentTimeMillis() + 1000 * 3600 * 24 * 7)));
        res.add(new Cookie("P00PRU", "1764530833", domain, "/", new Date(System.currentTimeMillis() + 1000 * 3600 * 24 * 7)));
        res.add(new Cookie("P01010", "1683216000", domain, "/", new Date(System.currentTimeMillis() + 1000 * 3600 * 24 * 7)));
        res.add(new Cookie("P1111129", "1683175255", domain, "/", new Date(System.currentTimeMillis() + 1000 * 3600 * 24 * 7)));
        return res;
    }

    private static List<Cookie> getYouKuCookies() {
        final String domain = ".youku.com";
        Cookie cookie1 = new Cookie("P_ck_ctl", "ED975295D088E933DFD5B98DC6CD24FE", domain, "/", new Date(System.currentTimeMillis() + 1000 * 3600 * 24 * 7));
        Cookie cookie2 = new Cookie("P_gck", "NA%7C5uBHh44t5hvtcTDW%2FlS%2FJw%3D%3D%7CNA%7C1683169146047", domain, "/", new Date(System.currentTimeMillis() + 1000 * 3600 * 24 * 7));
        Cookie cookie3 = new Cookie("P_pck_rm", "lNKfvOu4077afcd4b4e02eZBByUR7NRkAMzwtH%2FA%2BQk6TCWmA9HtH1QBHwOwnRVji3CLemTvfc%2By%2BeR96dWri2dcXDEKRfzc4FAk%2FtSHPcLE86%2FHJsVAmnloZda9n7GoF0cgADjLxI3O0wx6cGlu19m0tMPGKCXUnNy%2B7A%3D%3D%5FV2", domain, "/", new Date(System.currentTimeMillis() + 1000 * 3600 * 24 * 7));
        Cookie cookie4 = new Cookie("P_sck", "lbNzyTpDU9Ch7WKTEJdy%2B5PQhisKjkbZgHPtDdbCz5mAj%2FG3upENQfDqXq550jG8ySq5Gbp0M9t9%2FFVkuDTvbuw6fmBkwISiU8ZqdvBJHiUHa3tSeq%2FYgY9rjWPkT0Aq1ADXAAVEhqiSA%2BwHU3gzgA%3D%3D", domain, "/", new Date(System.currentTimeMillis() + 1000 * 3600 * 24 * 7));
        return Lists.newArrayList(cookie1, cookie2, cookie3, cookie4);
    }

    private static String readScript(File file) throws IOException {
        return new String(Files.readAllBytes(Paths.get(file.getAbsolutePath())));
    }

}
