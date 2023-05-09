package com.ambitious.v2.constant;

/**
 * 下载器类型
 * @author ambitious
 * @date 2023/5/4
 */
public enum DownloaderType {

    /**
     * 单线程下载器
     */
    SIMPLE("simple"),
    /**
     * 多线程下载器
     */
    MULTI_THREAD("multi-thread")
    ;

    public final String value;

    DownloaderType(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
