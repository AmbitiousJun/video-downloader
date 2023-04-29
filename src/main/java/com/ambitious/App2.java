package com.ambitious;

import cn.hutool.http.HttpUtil;

import java.io.File;

/**
 * @author ambitious
 * @date 2023/4/28
 */
public class App2 {

    public static final String DL_LINK = "https://om.tc.qq.com/video.dispatch.tc.qq.com/gzc_1000102_0b53tuaamaaamqaeh6hmujrmbhoda2iaaasa.f10218.mp4?vkey=A691192B6B77EF5FFF4871DE5021D673538BDEC8AF90DD825D7FBC637129E9FCADDE31481958010B5CF2161C67AC9EE29283779B165A1DD109183A27CBDC471C0F107940724E83472559BCC6821B0AA942E4A2F31954EECEC0A786D00F9CA8A59EC1359BDD90F78FF3FDE70EFCB19800CAD4D3ABB26E6AF74E3637A8A0EEFEC8&sdtfrom=v1010&type=m3u8&platform=10201&br=117&fmt=10218&ver=0&sp=1&guid=46246e56b02d4b9a&cpro=29&cisp=1&stdfrom=1100&sdtfrom=v1010&type=mp4&platform=10901&br=117&fmt=10217&ver=0&sp=1&guid=bd719a7d3e1ad5f6";

    public static void main(String[] args) {
        HttpUtil.downloadFile(DL_LINK, new File("/Users/ambitious/Downloads/这是测试视频.mp4"));
    }

}
