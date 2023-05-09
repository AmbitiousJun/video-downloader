package com.ambitious.v2.constant;

/**
 * 程序支持的媒体类型
 * @author ambitious
 * @date 2023/5/5
 */
public enum MediaType {

    /**
     * mp4, m3u8
     */
    MP4("mp4"),
    M3U8("m3u8")
    ;

    public final String value;

    MediaType(java.lang.String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
