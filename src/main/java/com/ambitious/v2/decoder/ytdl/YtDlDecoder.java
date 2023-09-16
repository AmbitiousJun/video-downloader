package com.ambitious.v2.decoder.ytdl;

import com.ambitious.v2.config.Config;
import com.ambitious.v2.pojo.YtDlFormatCode;
import com.ambitious.v2.util.LogUtils;
import com.ambitious.v2.util.SleepUtils;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        LogUtils.error(LOGGER, String.format("解析失败，地址：%s", url));
        throw new RuntimeException("解析失败");
    }
}
