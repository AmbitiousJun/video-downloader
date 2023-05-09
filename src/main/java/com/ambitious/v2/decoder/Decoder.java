package com.ambitious.v2.decoder;

import com.ambitious.v2.config.Config;
import com.ambitious.v2.config.decoder.FreeApiConfig;
import com.ambitious.v2.constant.DecoderType;
import com.ambitious.v2.decoder.selenium.FreeApiSeleniumDecoder;
import com.ambitious.v2.decoder.selenium.SeleniumDecoder;
import com.ambitious.v2.decoder.selenium.VipFetchSeleniumDecoder;
import com.ambitious.v2.pojo.DownloadMeta;
import com.ambitious.v2.pojo.VideoMeta;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

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
            LOGGER.info("开始监听解析列表");
            int size = list.size();
            final AtomicInteger finish = new AtomicInteger(0);
            try {
                while (true) {
                    while (list.size() == 0) {
                        if (finish.get() == size) {
                            LOGGER.info("所有文件已解析完成，请耐心等待下载结束...");
                            return;
                        }
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    // 1 获取链接地址
                    VideoMeta meta = list.pop();
                    String name = meta.getName();
                    String url = meta.getUrl();
                    String fileName = Config.DOWNLOADER.DOWNLOAD_DIR + "/" + name + ".mp4";
                    LOGGER.info("检测到解析任务，标题：{}，源地址：{}", name, url);
                    // 2 判断解析类型
                    DecoderType use = Config.DECODER.USE;
                    try {
                        if (DecoderType.NONE.equals(use)) {
                            // 不需要解析，url 直接就是下载链接
                            listener.newDecodeUrl(new DownloadMeta(url, fileName));
                            continue;
                        }
                        if (DecoderType.FREE_API.equals(use)) {
                            if (decoder == null) {
                                decoder = new FreeApiSeleniumDecoder();
                            }
                            listener.newDecodeUrl(useFreeApiDecode(meta, fileName));
                        }
                        if (DecoderType.VIP_FETCH.equals(use)) {
                            if (decoder == null) {
                                decoder = new VipFetchSeleniumDecoder();
                            }
                            listener.newDecodeUrl(useVipFetchDecode(meta, fileName));
                        }
                        int current = finish.incrementAndGet();
                        LOGGER.info("已解析完成视频数：{}，剩余：{}", current, size - current);
                    } catch (Exception e) {
                        LOGGER.error("视频下载地址解析失败，重新加入任务列表", e);
                        list.offerLast(meta);
                    }
                }
            } finally {
                if (decoder != null) {
                    try {
                        decoder.close();
                        LOGGER.info("解析器成功关闭");
                    } catch (IOException e) {
                        throw new RuntimeException("解析器关闭失败", e);
                    }
                }
            }
        }, "t-decode-listening").start();
    }

    /**
     * 调用视频网站解析器解析
     * @param meta 视频信息
     * @param fileName 文件要下载到本地哪个位置
     * @return 下载信息
     */
    private static DownloadMeta useVipFetchDecode(VideoMeta meta, String fileName) {
        LOGGER.info("开始解析视频，标题：{}, 源地址：{}", meta.getName(), meta.getUrl());
        String link = decoder.fetchDownloadLink(meta.getUrl());
        LOGGER.info("解析成功，已添加到下载列表，文件名：{}，下载地址：{}", fileName, link);
        // 添加到下载列表
        return new DownloadMeta(link, fileName);
    }

    /**
     * 调用免费接口解析器进行解析
     * @param meta 要解析的视频信息
     * @param fileName 文件要下载到本地哪个位置
     * @return 下载信息
     */
    private static DownloadMeta useFreeApiDecode(VideoMeta meta, String fileName) {
        LOGGER.info("开始解析视频，标题：{}, 源地址：{}", meta.getName(), meta.getUrl());
        FreeApiConfig freeApiConfig = Config.DECODER.FREE_API;
        String baseUrl = freeApiConfig.APIS.get(freeApiConfig.USE);
        String finalUrl = baseUrl + meta.getUrl();
        String link = decoder.fetchDownloadLink(finalUrl);
        Map<String, String> headerMap = Maps.newHashMap();
        headerMap.put("origin", baseUrl.substring(0, baseUrl.substring(8).indexOf("/") + 8));
        headerMap.put("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/111.0.0.0 Safari/537.36");
        LOGGER.info("解析成功，已添加到下载列表，文件名：{}，下载地址：{}", fileName, link);
        // 添加到下载列表
        return new DownloadMeta(link, fileName, headerMap);
    }
}
