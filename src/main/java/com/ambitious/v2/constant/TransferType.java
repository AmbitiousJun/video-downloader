package com.ambitious.v2.constant;

/**
 * 转码器类型
 * @author ambitious
 * @date 2023/5/4
 */
public enum TransferType {

    /**
     * file-channel, cv, ffmpeg
     */
    FILE_CHANNEL("file-channel"),
    CV("cv"),
    FFMPEG("ffmpeg")
    ;

    public final String value;

    TransferType(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
