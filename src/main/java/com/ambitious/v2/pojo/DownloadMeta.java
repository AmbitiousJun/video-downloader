package com.ambitious.v2.pojo;

import com.ambitious.v2.util.HttpUtils;
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
        Map<String, String> m = HttpUtils.genDefaultHeaderMapByUrl(headerMap, link);
        this.link = link;
        this.fileName = fileName;
        this.originUrl = originUrl;
        this.headerMap = m;
    }
}
