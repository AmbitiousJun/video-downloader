package com.ambitious.v2.decoder.selenium;

import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 初始化爱奇艺的 Cookie
 * @author ambitious
 * @date 2023/5/7
 */
public class QyInitializer implements SiteInitializer {

    /**
     * 标记初始化是否完成
     */
    private volatile boolean flag = false;
    private final ChromeDriver driver;
    private final List<Cookie> cookies;

    public QyInitializer(ChromeDriver driver, List<Cookie> cookies) {
        this.driver = driver;
        this.cookies = cookies;
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(QyInitializer.class);

    @Override
    public void init(String url) {
        try {
            // 1 检查是否已经初始化过
            if (checkInit()) {
                driver.get(url);
                Thread.sleep(10000);
                return;
            }
            // 2 进入网页，设置 Cookie
            LOGGER.info("正在设置 Cookie 信息...");
            driver.get(url);
            for (Cookie cookie : cookies) {
                driver.manage().addCookie(cookie);
            }
            LOGGER.info("Cookie 信息设置完成");
            // 3 重新加载网页，让 Cookie 生效
            LOGGER.info("正在等待 Cookie 信息生效...");
            driver.navigate().refresh();
            Thread.sleep(10000);
            driver.navigate().refresh();
            Thread.sleep(15000);
            // 4 查找网页中隐藏的清晰度选择器，将它设置为显示状态
            LOGGER.info("正在查找网页中的清晰度设置框...");
            WebElement panel = driver.findElement(By.cssSelector("iqpdiv[data-player-hook='definitionPanel']"));
            String script = "arguments[0].setAttribute('style', 'display: block')";
            driver.executeScript(script, panel);
            Thread.sleep(200);
            // 5 切换到 1080P 清晰度
            LOGGER.info("正在切换到 1080P 清晰度...");
            Actions actions = new Actions(driver);
            WebElement btn = driver.findElement(By.cssSelector("iqp[class='iqp-txt-stream'][data-player-hook='5']"));
            actions.moveToElement(btn).click().perform();
            // 6 等待清晰度切换完成
            Thread.sleep(15000);
            LOGGER.info("爱奇艺客户端初始化完成");
            this.flag = true;
        } catch (Exception e) {
            throw new RuntimeException("初始化爱奇艺失败", e);
        }
    }

    /**
     * 检查是否初始化完成，
     * volatile + 双重检查锁，确保不会重复初始化
     * @return 是否初始化过
     */
    private boolean checkInit() {
        if (!this.flag) {
            synchronized (this) {
                return this.flag;
            }
        }
        return true;
    }
}
