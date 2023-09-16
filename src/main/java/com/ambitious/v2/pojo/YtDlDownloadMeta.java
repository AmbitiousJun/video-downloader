package com.ambitious.v2.pojo;
import java.util.*;

import cn.hutool.core.util.IdUtil;
import com.google.common.collect.Lists;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 构造一个适用于 youtube-dl 解析器的 DownloadMeta
 * @author ambitious
 * @date 2023/9/16
 */
@Data
@NoArgsConstructor
public class YtDlDownloadMeta {

    private List<String> links;
    private String fileName;
    private String originUrl;
    private Map<String, String> headerMap;
    public static final String SEGMENT = IdUtil.simpleUUID();

    public YtDlDownloadMeta(List<String> links, String fileName, String originUrl) {
        this.links = links;
        this.fileName = fileName;
        this.originUrl = originUrl;
    }

    public YtDlDownloadMeta(List<String> links, String fileName, String originUrl, Map<String, String> headerMap) {
        this(links, fileName, originUrl);
        this.headerMap = headerMap;
    }

    public DownloadMeta toDownloadMeta() {
        return new DownloadMeta(String.join(SEGMENT, links), fileName, originUrl, headerMap);
    }

    public static YtDlDownloadMeta recoverFromDownloadMeta(DownloadMeta meta) {
        YtDlDownloadMeta current = new YtDlDownloadMeta();
        current.setLinks(Lists.newArrayList(meta.getLink().split(SEGMENT)));
        current.setFileName(meta.getFileName());
        current.setOriginUrl(meta.getOriginUrl());
        current.setHeaderMap(meta.getHeaderMap());
        return current;
    }
}
