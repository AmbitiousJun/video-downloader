package com.ambitious.v2.config.decoder;

import com.ambitious.v2.util.CastUtils;
import com.ambitious.v2.util.NumberUtils;
import com.google.common.collect.Maps;
import lombok.Data;

import java.util.*;

/**
 * 免费解析接口配置
 * @author ambitious
 * @date 2023/5/4
 */
@Data
public class FreeApiConfig {

    public final String USE;
    public final Map<String, String> APIS;
    public final List<String> VALID_URL_PREFIXES;
    public final Integer WAIT_SECONDS;

    public FreeApiConfig(Map<String, Object> c) {
        try {
            String use = (String) c.get("use");
            int waitSeconds = CastUtils.cast(c.get("wait-seconds"));
            List<String> apis = CastUtils.cast(c.get("apis"));
            boolean valid = false;
            Map<String, String> apiMap = Maps.newHashMap();
            for (String s : apis) {
                String[] ss = s.split(",");
                if (ss.length != 2) {
                    throw new RuntimeException("接口格式：name,url");
                }
                apiMap.put(ss[0], ss[1]);
                valid = ss[0].equals(use) || valid;
            }
            if (!valid) {
                throw new RuntimeException("请确保指定的接口存在于解析接口列表中");
            }
            this.APIS = apiMap;
            this.USE = use;
            this.VALID_URL_PREFIXES = CastUtils.cast(c.get("valid-url-prefixes"));
            this.WAIT_SECONDS = waitSeconds < 0 ? 30 : waitSeconds;
        } catch (Exception e) {
            throw new RuntimeException("加载 free-api 配置失败", e);
        }
    }
}
