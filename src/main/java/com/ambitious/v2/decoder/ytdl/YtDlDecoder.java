package com.ambitious.v2.decoder.ytdl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.ambitious.v2.config.Config;
import com.ambitious.v2.pojo.YtDlFormatCode;
import com.ambitious.v2.util.LogUtils;
import com.ambitious.v2.util.SleepUtils;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author ambitious
 * @date 2023/9/16
 */
@Slf4j
public class YtDlDecoder {

    public static final Logger LOGGER = LoggerFactory.getLogger(YtDlDecoder.class);

    /**
     * 每个 format code 至多解析 3 次
     */
    public static final Integer RETRY_TIME = 3;

    /**
     * 使用 youtube-dl 解析视频下载地址
     * @param url 视频地址
     * @return links
     */
    public static List<String> fetchDownloadLinks(String url) {
        List<YtDlFormatCode> codes = Config.DECODER.YOUTUBE_DL.formatCodes;
        // 1 尝试配置文件中配置的 format
        List<String> links = tryLinks(url, codes);
        if (CollectionUtil.isNotEmpty(links)) {
            return links;
        }
        // 2 尝试用户手动输入的 format
        System.out.println(LogUtils.packMsg(LogUtils.ANSI_WARNING, "预置 code 全部解析失败或没有配置，触发手动选择，url：" + url));
        YtDlFormatCode code = new YtDlCodeSelector(url).requestCode();
        links = tryLinks(url, Collections.singletonList(code));
        if (CollectionUtil.isNotEmpty(links)) {
            return links;
        }
        LogUtils.error(LOGGER, String.format("解析失败，地址：%s", url));
        throw new RuntimeException("解析失败");
    }

    public static List<String> tryLinks(String url, List<YtDlFormatCode> codes) {
        for (YtDlFormatCode code : codes) {
            int currentTry = 1;
            while (currentTry <= RETRY_TIME) {
                LogUtils.info(LOGGER, String.format("尝试解析地址：%s, format code: %s, 第 %d 次尝试...", url, code.getCode(), currentTry));
                try {
                    return new YtDlHandler(url, code).getLinks();
                } catch (Exception e) {
                    currentTry++;
                    SleepUtils.sleep(1000);
                }
            }
        }
        return null;
    }
}
