package com.ambitious.v2.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 记录每个 Ts 文件的元信息
 * @author ambitious
 * @date 2023/4/29
 */
@Data
@AllArgsConstructor
public class TsMeta {

    /**
     * 真实请求 url
     */
    private String url;
    /**
     * 记录 ts 文件是位于第几个，便于后期合成
     */
    private Integer index;
}
