package com.ambitious.v2.constant;

/**
 * 解析器类型
 * @author ambitious
 * @date 2023/5/4
 */
public enum DecoderType {

    /**
     * none, free-api, vip-fetch
     */
    NONE("none"),
    FREE_API("free-api"),
    VIP_FETCH("vip-fetch")
    ;

    public final String value;

    DecoderType(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
