package com.ambitious.v2.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author ambitious
 * @date 2023/9/16
 */
@Data
@AllArgsConstructor
public class YtDlFormatCode {

    private String code;
    private Integer expectedLinkNums;
}
