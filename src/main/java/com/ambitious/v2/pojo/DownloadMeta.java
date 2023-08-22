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
@AllArgsConstructor
public class DownloadMeta {

    private String link;
    private String fileName;
    private String originUrl;
    private Map<String, String> headerMap;

    public DownloadMeta(String link, String fileName, String originUrl) {
        // 根据 link 添加默认的 Referer
        Map<String, String> m = null;
        final String mg = "mgtv.com";
        if (link.contains(mg)) {
            m = Maps.newHashMap();
            m.put("Referer", "https://" + mg);
        }
        this.link = link;
        this.fileName = fileName;
        this.originUrl = originUrl;
        this.headerMap = m;
    }
}
