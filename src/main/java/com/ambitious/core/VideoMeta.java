package com.ambitious.core;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 视频文件源数据
 * @author ambitious
 * @date 2023/4/28
 */
@Data
@AllArgsConstructor
public class VideoMeta {

    private String name;
    private String url;
}
