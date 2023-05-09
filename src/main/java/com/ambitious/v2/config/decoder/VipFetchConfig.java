package com.ambitious.v2.config.decoder;

import cn.hutool.core.collection.CollectionUtil;
import com.ambitious.v2.constant.VideoSite;
import com.ambitious.v2.util.CastUtils;
import com.google.common.collect.Maps;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Vip 视频抓取登录态配置
 * @author ambitious
 * @date 2023/5/4
 */
@Data
public class VipFetchConfig {

    public final String USE;
    public final String DOMAIN;
    public final Map<String, String> COOKIES;

    public VipFetchConfig(Map<String, Object> c) {
        try {
            String use = (String) c.get("use");
            String domain = null;
            List<String> cookieList = null;
            Map<String, Object> sites = CastUtils.cast(c.get("sites"));
            for (String validSite : VideoSite.VALID_SITES) {
                // 匹配需要下载的视频网站
                if (validSite.equals(use)) {
                    Map<String, Object> site = CastUtils.cast(sites.get(use));
                    // 获取该视频网站的 Cookies
                    domain = (String) site.get("domain");
                    cookieList = CastUtils.cast(site.get("cookies"));
                    break;
                }
            }
            this.USE = use;
            this.DOMAIN = domain;
            this.COOKIES = Maps.newHashMap();
            if (CollectionUtil.isNotEmpty(cookieList)) {
                for (String cookie : cookieList) {
                    String[] ss = cookie.split(",");
                    if (ss.length != 2) {
                        throw new RuntimeException("Cookie 正确格式：key,value");
                    }
                    this.COOKIES.put(ss[0], ss[1]);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("初始化 vip-fetch 配置失败", e);
        }
    }
}
