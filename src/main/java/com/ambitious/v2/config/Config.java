package com.ambitious.v2.config;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import com.ambitious.v2.config.decoder.DecoderConfig;
import com.ambitious.v2.constant.DecoderType;
import com.ambitious.v2.constant.DownloaderType;
import com.ambitious.v2.constant.MediaType;
import com.ambitious.v2.constant.TransferType;
import com.ambitious.v2.util.CastUtils;
import com.ambitious.v2.util.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;

/**
 * 存储用户在 config.yml 文件中的配置
 * @author ambitious
 * @date 2023/5/4
 */
public class Config {

    public static final SeleniumConfig SELENIUM;
    public static final DownloaderConfig DOWNLOADER;
    public static final TransferConfig TRANSFER;
    public static final DecoderConfig DECODER;

    private static final Logger LOGGER = LoggerFactory.getLogger(Config.class);

    static {
        try {
            LOGGER.info("正在加载配置...");
            // 加载配置
            Map<String, Object> c = loadConfigMap();
            SELENIUM = readSeleniumConfig(CastUtils.cast(c.get("selenium")));
            DOWNLOADER = readDownloaderConfig(CastUtils.cast(c.get("downloader")));
            TRANSFER = readTransferConfig(CastUtils.cast(c.get("transfer")));
            DECODER = readDecoderConfig(CastUtils.cast(c.get("decoder")));
            LOGGER.info("配置加载完成");
        } catch (Exception e) {
            throw new RuntimeException("加载配置文件异常", e);
        }
    }

    public static void load() {}

    /**
     * 读取 解析器 配置
     * @param c 配置对象
     */
    private static DecoderConfig readDecoderConfig(Map<String, Object> c) {
        if (c == null) {
            throw new RuntimeException("decoder 配置为空");
        }
        DecoderType use;
        try {
            String useStr = (String) c.get("use");
            use = DecoderType.valueOf(useStr.toUpperCase().replaceAll("-", "_"));
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("解析器类型配置错误，可选值：none, free-api, vip-fetch");
        }
        MediaType resourceType;
        try {
            String rtStr = (String) c.get("resource-type");
            resourceType = MediaType.valueOf(rtStr.toUpperCase().replaceAll("-", "_"));
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("媒体类型配置错误，可选值：mp4, m3u8");
        }
        int threadCount = CastUtils.cast(c.get("thread-count"));
        return new DecoderConfig(use, resourceType, threadCount, c);
    }

    /**
     * 读取 转码器 配置
     * @param c 配置对象
     */
    private static TransferConfig readTransferConfig(Map<String, Object> c) {
        TransferType use;
        try {
            String useStr = (String) c.get("use");
            use = TransferType.valueOf(useStr.toUpperCase().replaceAll("-", "_"));
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("转码器类型配置错误，可选值：file-channel, cv, ffmpeg");
        }
        String tsFilenameRegex = (String) c.get("ts-filename-regex");
        return new TransferConfig(use, tsFilenameRegex);
    }

    /**
     * 读取 下载器 配置
     * @param c 配置对象
     */
    private static DownloaderConfig readDownloaderConfig(Map<String, Object> c) {
        DownloaderType use;
        try {
            String useStr = (String) c.get("use");
            use = DownloaderType.valueOf(useStr.toUpperCase().replaceAll("-", "_"));
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("下载器类型配置错误，可选值：simple, multi-thread");
        }
        int taskThreadCount = CastUtils.cast(c.get("task-thread-count"));
        int dlThreadCount = CastUtils.cast(c.get("dl-thread-count"));
        String downloadDir = (String) c.get("download-dir");
        String tsDirSuffix = (String) c.get("ts-dir-suffix");
        return new DownloaderConfig(use, taskThreadCount, dlThreadCount, downloadDir, tsDirSuffix);
    }

    /**
     * 读取 selenium 配置
     * @param c 配置对象
     */
    private static SeleniumConfig readSeleniumConfig(Map<String, Object> c) {
        if (c == null) {
            throw new RuntimeException("selenium 配置为空");
        }
        String chromeDriverPath = CastUtils.cast(c.get("chrome-driver-path"));
        Object sw = c.get("show-window");
        if (!(sw instanceof Boolean)) {
            sw = false;
        }
        return new SeleniumConfig(chromeDriverPath, (Boolean) sw);
    }

    /**
     * 将配置文件加载成 HashMap
     * @return HashMap
     */
    private static Map<String, Object> loadConfigMap() throws IOException {
        Yaml yaml = new Yaml();
        try (InputStream is = Config.class.getClassLoader().getResourceAsStream("config.yml")) {
            return yaml.load(is);
        }
    }
}
