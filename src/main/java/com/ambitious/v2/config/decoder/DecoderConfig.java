package com.ambitious.v2.config.decoder;

import cn.hutool.core.util.StrUtil;
import com.ambitious.v2.constant.DecoderType;
import com.ambitious.v2.constant.MediaType;
import com.ambitious.v2.util.CastUtils;
import lombok.Data;

import java.util.Map;

/**
 * 解析器配置
 * @author ambitious
 * @date 2023/5/4
 */
@Data
public class DecoderConfig {

    public final DecoderType USE;
    public final MediaType RESOURCE_TYPE;
    public final Integer THREAD_COUNT;
    public final YoutubeDlConfig YOUTUBE_DL;
    public final FreeApiConfig FREE_API;
    public final VipFetchConfig VIP_FETCH;

    public DecoderConfig(DecoderType use, MediaType resourceType, int threadCount, Map<String, Object> c) {
        this.USE = use == null ? DecoderType.NONE : use;
        this.RESOURCE_TYPE = resourceType;
        this.THREAD_COUNT = threadCount < 0 ? 1 : threadCount;
        this.YOUTUBE_DL = DecoderType.YOUTUBE_DL.equals(this.USE) ? new YoutubeDlConfig(CastUtils.cast(c.get("youtube-dl"))) : null;
        this.FREE_API = DecoderType.FREE_API.equals(this.USE) ? new FreeApiConfig(CastUtils.cast(c.get("free-api"))) : null;
        this.VIP_FETCH = DecoderType.VIP_FETCH.equals(this.USE) ? new VipFetchConfig(CastUtils.cast(c.get("vip-fetch"))) : null;
    }
}
