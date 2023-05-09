package com.ambitious.v2.constant;

import com.google.common.collect.Lists;

import java.util.List;

/**
 * 视频网站
 * @author ambitious
 * @date 2023/5/4
 */
public interface VideoSite {

    String QY = "qy";
    String TX = "tx";
    String YK = "yk";
    String MG = "mg";
    List<String> VALID_SITES = Lists.newArrayList(QY, TX, YK, MG);
}
