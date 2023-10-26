package com.ambitious.v2.util;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.IdUtil;
import com.ambitious.v2.config.Config;
import com.google.common.collect.Maps;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * @author ambitious
 * @date 2023/5/8
 */
public class HttpUtils {


    public static final Logger LOGGER = LoggerFactory.getLogger(HttpUtils.class);
    /**
     * 建立连接的超时时间
     */
    public static final Integer CONNECT_TIMEOUT = 30 * 1000;
    /**
     * 读取数据的超时时间
     */
    public static final Integer READ_TIMEOUT = 120 * 1000;

    /**
     * 生成 HttpConnection 的参数
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class HttpOptions {
        private String url;
        private String method = "GET";
        private Map<String, String> headers;

        public HttpOptions(String url, Map<String, String> headers) {
            this.url = url;
            this.headers = headers;
        }
    }

    /**
     * 关闭 Http 连接
     * @param connection connection
     */
    public static void closeConn(HttpURLConnection connection) {
        if (connection != null) {
            connection.disconnect();
        }
    }

    /**
     * 生成一个带有默认 header 头的 headerMap
     * @param baseMap 已存在的 map
     * @param url 需要检测的 url
     * @return headerMap
     */
    public static Map<String, String> genDefaultHeaderMapByUrl(Map<String, String> baseMap, String url) {
        Map<String, String> m = baseMap == null ? Maps.newHashMap() : baseMap;
        final String mg = "mgtv.com";
        final String bili = "bilivideo";
        if (url.contains(mg)) {
            m.put("Referer", "https://" + mg);
        }
        if (url.contains(bili)) {
            m.put("Referer", "https://bilibili.com");
        }
        return m;
    }

    /**
     * 生成一个 Http 请求 URL
     * @param options 请求参数
     * @return HttpURLConnection
     */
    public static HttpURLConnection genHttpConnection(HttpOptions options) throws IOException {
        String url = options.getUrl();
        String method = options.getMethod();
        Map<String, String> headers = options.getHeaders();
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod(method);
        if (CollectionUtil.isNotEmpty(headers)) {
            for (String key : headers.keySet()) {
                conn.setRequestProperty(key, headers.get(key));
            }
        }
        conn.setConnectTimeout(CONNECT_TIMEOUT);
        conn.setReadTimeout(READ_TIMEOUT);
        return conn;
    }

    /**
     * 将输入流输出到文件中，根据类上的读取超时常量进行超时控制
     * @param is 输入流
     * @param dest 目标文件
     */
    public static void downloadStream2File(InputStream is, File dest, long contentLength) throws IOException {
        String traceId = IdUtil.simpleUUID();
        // LogUtils.warning(LOGGER, String.format("流大小：%s, traceId: %s", contentLength, traceId));
        LogUtils.info(LOGGER, String.format("正在下载流，大小：%.2f MB, 文件名：%s", Long.valueOf(contentLength).doubleValue() / 1024 / 1024, dest.getName()));
        try (BufferedOutputStream bos = new BufferedOutputStream(Files.newOutputStream(dest.toPath()));
             BufferedInputStream bis = new BufferedInputStream(is)
        ) {
            // 缓冲区大小不能超过下载器的速率限制
            byte[] buffer = new byte[1024 * 1024];
            MyTokenBucket bucket = Config.DOWNLOADER.TOKEN_BUCKET;
            // 获取当前能够读取的字节数
            int len = bis.read(buffer, 0, bucket.tryConsume(buffer.length));
            LogUtils.warning(LOGGER, "首次读取得到的 len: " + len);
            while (len > 0) {
                // LogUtils.warning(LOGGER, String.format("consume：%s，读取到的字节数：%s，实际的字节数：%s", consume, len, Long.valueOf(contentLength).doubleValue()));
                bos.write(buffer, 0, len);
                LogUtils.warning(LOGGER, "成功写入 " + len + " 字节");
                len = bis.read(buffer, 0, bucket.tryConsume(buffer.length));
            }
            LogUtils.success(LOGGER, "下载结束");
        }
    }
}
