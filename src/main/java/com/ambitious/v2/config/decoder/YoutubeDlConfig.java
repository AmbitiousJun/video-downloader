package com.ambitious.v2.config.decoder;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.ambitious.v2.config.Config;
import com.ambitious.v2.pojo.YtDlFormatCode;
import com.ambitious.v2.util.CastUtils;
import com.google.common.collect.Lists;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * youtube-dl 解析器配置
 * @author ambitious
 * @date 2023/9/16
 */
public class YoutubeDlConfig {

    public final String cookiesFrom;
    public final List<YtDlFormatCode> formatCodes;
    public static final String NO_COOKIE = "none";

    public YoutubeDlConfig(Map<String, Object> c) {
        try {
            String cookiesFrom = (String) c.get("cookies-from");
            this.cookiesFrom = StrUtil.isEmpty(cookiesFrom) || cookiesFrom.equals(NO_COOKIE) ? null : cookiesFrom;
            String formatCodesKey = "format-codes";
            if (Objects.isNull(c.get(formatCodesKey))) {
                this.formatCodes = Lists.newArrayList();
            } else {
                List<Object> rawCodes = CastUtils.cast(c.get(formatCodesKey));
                this.formatCodes = checkFormatCodes(rawCodes);
            }
            checkLocalEnv();
        } catch (Exception e) {
            throw new RuntimeException("加载 youtube-dl 配置失败", e);
        }
    }

    /**
     * 检查本地环境中是否有 youtube-dl
     */
    private void checkLocalEnv() {
        ProcessBuilder builder = new ProcessBuilder(Config.YOUTUBE_DL_PATH, "--help");
        builder.redirectErrorStream(true);
        try {
            Process process = builder.start();
            BufferedReader bf = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String firstLine = bf.readLine();
            if (StrUtil.isEmpty(firstLine) || !firstLine.startsWith("Usage:")) {
                throw new RuntimeException();
            }
        } catch (Exception e) {
            throw new RuntimeException("检查 youtube-dl 环境失败，请确保系统中已经安装，yt-dlp 需改名 youtube-dl");
        }
    }

    private List<YtDlFormatCode> checkFormatCodes(List<Object> rawCodes) {
        if (CollectionUtil.isEmpty(rawCodes)) {
            throw new RuntimeException("必须指定一个或一个以上的 format code，可通过 youtube-dl -F 进行获取");
        }
        List<String> formatCodes = Lists.newArrayList();
        for (Object rawCode : rawCodes) {
            formatCodes.add(rawCode + "");
        }
        return formatCodes.stream().map(code -> {
            String[] cs = code.split("\\+");
            if (cs.length != 1 && cs.length != 2) {
                throw new RuntimeException("不合法的 format code：" + code + "，示例：137+140, 3");
            }
            return new YtDlFormatCode(code, cs.length);
        }).collect(Collectors.toList());
    }
}
