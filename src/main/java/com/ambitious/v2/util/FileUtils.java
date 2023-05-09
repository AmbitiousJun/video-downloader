package com.ambitious.v2.util;

import java.io.File;

/**
 * @author ambitious
 * @date 2023/5/7
 */
public class FileUtils {

    /**
     * 初始化文件的父目录
     * @param path 文件绝对路径
     */
    public static void initFileDirs(String path) {
        File file = new File(path);
        if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
            throw new RuntimeException("初始化文件目录失败");
        }
        if (file.exists() && !file.delete()) {
            throw new RuntimeException("文件已存在，无法覆盖: path=" + path);
        }
    }
}
