package com.ambitious.v2.decoder;

import cn.hutool.core.util.StrUtil;
import com.ambitious.v2.config.Config;
import com.ambitious.v2.config.decoder.FreeApiConfig;
import com.ambitious.v2.constant.DecoderType;
import com.ambitious.v2.decoder.selenium.FreeApiSeleniumDecoder;
import com.ambitious.v2.decoder.selenium.SeleniumDecoder;
import com.ambitious.v2.decoder.selenium.VipFetchSeleniumDecoder;
import com.ambitious.v2.decoder.ytdl.YtDlDecoder;
import com.ambitious.v2.pojo.DownloadMeta;
import com.ambitious.v2.pojo.VideoMeta;
import com.ambitious.v2.pojo.YtDlDownloadMeta;
import com.ambitious.v2.util.LogUtils;
import com.ambitious.v2.util.SleepUtils;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Deque;
import java.util.List;
import java.util.Map;

/**
 * 负责将源数据文件解析成 可下载的 URL
 * @author ambitious
 * @date 2023/5/7
 */
public class Decoder {

    private static final Logger LOGGER = LoggerFactory.getLogger(Decoder.class);
    private static SeleniumDecoder decoder;

    @SuppressWarnings("all")
    public static void listenAndDecode(Deque<VideoMeta> list, DecodeListener listener) {
        new Thread(() -> {
            LogUtils.info(LOGGER, "开始监听解析列表");
            try {
                while (true) {
                    while (list.size() == 0) {
                        SleepUtils.sleep(2000);
                    }
                    // 1 获取链接地址
                    VideoMeta meta = list.pop();
                    String name = meta.getName();
                    String url = meta.getUrl();
                    LogUtils.info(LOGGER, String.format("检测到解析任务，标题：%s，源地址：%s", name, url));
                    // 2 判断解析类型
                    DecoderType use = Config.DECODER.USE;
                    try {
                        if (DecoderType.NONE.equals(use)) {
                            // 不需要解析，url 直接就是下载链接
                            listener.newDecodeUrl(new DownloadMeta(url, name, meta.getUrl()));
                        } else if (DecoderType.YOUTUBE_DL.equals(use)) {
                            listener.newDecodeUrl(useYoutubeDlDecode(meta));
                        } else if (DecoderType.FREE_API.equals(use)) {
                            if (decoder == null) {
                                decoder = new FreeApiSeleniumDecoder();
                            }
                            listener.newDecodeUrl(useFreeApiDecode(meta, name));
                        } else if (DecoderType.VIP_FETCH.equals(use)) {
                            if (decoder == null) {
                                decoder = new VipFetchSeleniumDecoder();
                            }
                            listener.newDecodeUrl(useVipFetchDecode(meta, name));
                        }
                        SleepUtils.sleep(10000);
                    } catch (Exception e) {
                        LogUtils.error(LOGGER, "视频下载地址解析失败：" + e.getMessage() + "，重新加入任务列表");
                        list.offerLast(meta);
                    }
                }
            } finally {
                if (decoder != null) {
                    try {
                        decoder.close();
                        LogUtils.success(LOGGER, "解析器成功关闭");
                    } catch (IOException e) {
                        throw new RuntimeException("解析器关闭失败", e);
                    }
                }
            }
        }, "t-decode-listening").start();
    }

    /**
     * 使用 youtube-dl 进行解析
     * @param meta 视频信息
     * @return 下载相关信息
     */
    private static DownloadMeta useYoutubeDlDecode(VideoMeta meta) {
        LogUtils.info(LOGGER, String.format("开始解析视频，标题：%s, 源地址：%s", meta.getName(), meta.getUrl()));
        List<String> links = YtDlDecoder.fetchDownloadLinks(meta.getUrl());
        LogUtils.success(LOGGER, String.format("解析成功，已添加到下载列表，文件名：%s，下载地址：%s", meta.getName(), links));
        return new YtDlDownloadMeta(links, meta.getName(), meta.getUrl()).toDownloadMeta();
    }

    /**
     * 调用视频网站解析器解析
     * @param meta 视频信息
     * @param fileName 文件要下载到本地哪个位置
     * @return 下载信息
     */
    private static DownloadMeta useVipFetchDecode(VideoMeta meta, String fileName) {
        LogUtils.info(LOGGER, String.format("开始解析视频，标题：%s, 源地址：%s", meta.getName(), meta.getUrl()));
        String link = decoder.fetchDownloadLink(meta.getUrl());
        LogUtils.success(LOGGER, String.format("解析成功，已添加到下载列表，文件名：%s，下载地址：%s", fileName, link));
        // 添加到下载列表
        return new DownloadMeta(link, fileName, meta.getUrl());
    }

    /**
     * 调用免费接口解析器进行解析
     * @param meta 要解析的视频信息
     * @param fileName 文件要下载到本地哪个位置
     * @return 下载信息
     */
    private static DownloadMeta useFreeApiDecode(VideoMeta meta, String fileName) {
        LogUtils.info(LOGGER, String.format("开始解析视频，标题：%s, 源地址：%s", meta.getName(), meta.getUrl()));
        FreeApiConfig freeApiConfig = Config.DECODER.FREE_API;
        String baseUrl = freeApiConfig.APIS.get(freeApiConfig.USE);
        String finalUrl = baseUrl + meta.getUrl();
        String link = decoder.fetchDownloadLink(finalUrl);
        Map<String, String> headerMap = Maps.newHashMap();
        if (StrUtil.isNotEmpty(baseUrl)) {
            headerMap.put("origin", baseUrl.substring(0, baseUrl.substring(8).indexOf("/") + 8));
            headerMap.put("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/111.0.0.0 Safari/537.36");
        }
        LogUtils.success(LOGGER, String.format("解析成功，已添加到下载列表，文件名：%s，下载地址：%s", fileName, link));
        // 添加到下载列表
        return new DownloadMeta(link, fileName, meta.getUrl(), headerMap);
    }
}
