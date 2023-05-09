package com.ambitious.v2.config;

import cn.hutool.core.util.StrUtil;
import com.ambitious.v2.constant.TransferType;
import lombok.Data;

/**
 * 转码器配置
 * @author ambitious
 * @date 2023/5/4
 */
@Data
public class TransferConfig {

    public final TransferType USE;
    public final String TS_FILENAME_REGEX;

    public TransferConfig(TransferType use, String tsFilenameRegex) {
        this.USE = use == null ? TransferType.FFMPEG : use;
        this.TS_FILENAME_REGEX = StrUtil.isNotEmpty(tsFilenameRegex) ? tsFilenameRegex : "(?<=_)(\\d+)(?=\\.)";
    }
}
