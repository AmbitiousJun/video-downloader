package com.ambitious.v2.config;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import com.ambitious.v2.config.decoder.DecoderConfig;
import com.ambitious.v2.constant.DecoderType;
import com.ambitious.v2.constant.DownloaderType;
import com.ambitious.v2.constant.MediaType;
import com.ambitious.v2.constant.TransferType;
import com.ambitious.v2.util.CastUtils;
import com.ambitious.v2.util.LogUtils;
import com.ambitious.v2.util.NumberUtils;
import com.google.common.collect.Lists;
import com.google.common.primitives.Ints;
import org.bytedeco.ffmpeg.ffmpeg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
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
    public static final String FFMPEG_PATH;
    public static final String YOUTUBE_DL_PATH;

    private static final Logger LOGGER = LoggerFactory.getLogger(Config.class);

    static {
        try {
            LogUtils.info(LOGGER, "正在加载配置...");
            // 加载配置
            Map<String, Object> c = loadConfigMap();
            List<String> paths = readDependencyPaths(CastUtils.cast(c.get("os")));
            if (paths.size() != 2) {
                throw new RuntimeException("读取依赖执行路径异常");
            }
            FFMPEG_PATH = paths.get(0);
            YOUTUBE_DL_PATH = paths.get(1);
            SELENIUM = readSeleniumConfig(CastUtils.cast(c.get("selenium")));
            DOWNLOADER = readDownloaderConfig(CastUtils.cast(c.get("downloader")));
            TRANSFER = readTransferConfig(CastUtils.cast(c.get("transfer")));
            DECODER = readDecoderConfig(CastUtils.cast(c.get("decoder")));
            LogUtils.success(LOGGER, "配置加载完成");
        } catch (Exception e) {
            throw new RuntimeException("加载配置文件异常", e);
        }
    }

    /**
     * 读取 ffmpeg、youtube-dl 的可执行文件路径
     * @param os 用户运行的系统环境
     * @return 路径集合
     */
    private static List<String> readDependencyPaths(String os) {
        List<String> paths = Lists.newArrayList("ffmpeg", "youtube-dl");
        if (StrUtil.isEmpty(os)) {
            return paths;
        }
        String path = checkPath("config/ffmpeg/ffmpeg-" + os);
        if (StrUtil.isNotEmpty(path)) {
            paths.set(0, path);
        }
        path = checkPath("config/youtube-dl/youtube-dl-" + os);
        if (StrUtil.isNotEmpty(path)) {
            paths.set(1, path);
        }
        return paths;
    }

    /**
     * 检查路径的文件是否存在
     * @param path 要检查的路径
     * @return 检测成功的路径
     */
    private static String checkPath(String path) {
        List<String> validExtensions = Lists.newArrayList("", ".exe", ".sh", ".cmd");
        for (String ext : validExtensions) {
            File file = new File(path + ext);
            if (file.exists()) {
                return path + ext;
            }
        }
        return null;
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
        return new DecoderConfig(use, resourceType, c);
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
        String rateLimit = (String) c.get("rate-limit");
        return new DownloaderConfig(use, taskThreadCount, dlThreadCount, downloadDir, tsDirSuffix, rateLimit);
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
        try (InputStream is = Files.newInputStream(Paths.get("config/config.yml"))) {
            return yaml.load(is);
        }
    }
}
