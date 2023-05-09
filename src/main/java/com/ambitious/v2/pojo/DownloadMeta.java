package com.ambitious.v2.pojo;

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
    private Map<String, String> headerMap;

    public DownloadMeta(String link, String fileName) {
        this(link, fileName, null);
    }
}
