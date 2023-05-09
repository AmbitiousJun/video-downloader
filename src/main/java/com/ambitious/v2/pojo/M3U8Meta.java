package com.ambitious.v2.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.File;
import java.util.Deque;

/**
 * 存放 m3u8 文件的元信息
 * @author ambitious
 * @date 2023/5/1
 */
@Data
@AllArgsConstructor
public class M3U8Meta {

    private String fileName;
    private File tempDir;
    private Deque<TsMeta> tsUrls;
}
