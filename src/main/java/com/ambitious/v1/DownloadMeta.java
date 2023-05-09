package com.ambitious.v1;

import lombok.AllArgsConstructor;
import lombok.Data;

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
}
