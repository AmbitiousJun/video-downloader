package com.ambitious.v2.util;

import cn.hutool.core.collection.CollectionUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
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
        long startTime = System.currentTimeMillis();
        LogUtils.info(LOGGER, String.format("正在下载流，大小：%.2f MB, 文件名：%s", Long.valueOf(contentLength).doubleValue() / 1024 / 1024, dest.getName()));
        try (FileOutputStream fos = new FileOutputStream(dest);
             BufferedInputStream bis = new BufferedInputStream(is)
        ) {
            byte[] buffer = new byte[Long.valueOf(contentLength).intValue() / 2 + 1];
            long curTime = System.currentTimeMillis();
            if (curTime - startTime > READ_TIMEOUT) {
                throw new RuntimeException("下载超时");
            }
            int len = bis.read(buffer, 0, buffer.length);
            while (len > 0) {
                fos.write(buffer, 0, len);
                len = bis.read(buffer, 0, buffer.length);
            }
        }
    }
}
