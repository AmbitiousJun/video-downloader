package com.ambitious.v2.pojo;

import com.google.common.collect.Maps;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

/**
 * 视频下载元数据
 * @author ambitious
 * @date 2023/4/28
 */
@Data
public class DownloadMeta {

    private String link;
    private String fileName;
    private String originUrl;
    private Map<String, String> headerMap;

    public DownloadMeta(String link, String fileName, String originUrl) {
        this(link, fileName, originUrl, null);
    }

    public DownloadMeta(String link, String fileName, String originUrl, Map<String, String> headerMap) {
        // 根据 link 添加默认的 Referer
        Map<String, String> m = headerMap == null ? Maps.newHashMap() : headerMap;
        final String mg = "mgtv.com";
        final String bili = "bilivideo.com";
        if (link.contains(mg)) {
            m.put("Referer", "https://" + mg);
        }
        if (link.contains(bili)) {
            m.put("Referer", "https://bilibili.com");
        }
        this.link = link;
        this.fileName = fileName;
        this.originUrl = originUrl;
        this.headerMap = m;
    }
}
