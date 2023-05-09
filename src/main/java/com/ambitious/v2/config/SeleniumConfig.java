package com.ambitious.v2.config;

import cn.hutool.core.util.StrUtil;
import lombok.Data;

/**
 * selenium 配置
 * @author ambitious
 * @date 2023/5/4
 */
@Data
public class SeleniumConfig {

    public final String CHROME_DRIVER_PATH;
    public final Boolean SHOW_WINDOW;

    public SeleniumConfig(String chromeDriverPath, boolean showWindow) {
        if (StrUtil.isEmpty(chromeDriverPath)) {
            throw new RuntimeException("ChromeDriver 路径为空");
        }
        this.CHROME_DRIVER_PATH = chromeDriverPath;
        this.SHOW_WINDOW = showWindow;
    }
}

