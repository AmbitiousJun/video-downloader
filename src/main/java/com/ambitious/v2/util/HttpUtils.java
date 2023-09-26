package com.ambitious.v2.util;

import cn.hutool.core.collection.CollectionUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;
import java.util.Optional;

/**
 * @author ambitious
 * @date 2023/5/8
 */
public class HttpUtils {

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
        private Integer connectTimeout = 30 * 1000;
        private Integer readTimeout = 120 * 1000;

        public HttpOptions(String url, String method, Map<String, String> headers) {
            this(url, headers);
            this.method = method;
        }

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
        Integer readTimeout = options.getReadTimeout();
        Integer connectTimeout = options.getConnectTimeout();
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod(method);
        if (CollectionUtil.isNotEmpty(headers)) {
            for (String key : headers.keySet()) {
                conn.setRequestProperty(key, headers.get(key));
            }
        }
        conn.setConnectTimeout(connectTimeout);
        conn.setReadTimeout(readTimeout);
        return conn;
    }

    /**
     * 将输入流输出到文件中
     * @param is 输入流
     * @param dest 目标文件
     */
    public static void downloadStream2File(InputStream is, File dest) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(dest);
             BufferedInputStream bis = new BufferedInputStream(is)
        ) {
            byte[] buffer = new byte[1024 * 1024];
            int len = bis.read(buffer, 0, buffer.length);
            while (len > 0) {
                fos.write(buffer, 0, len);
                len = bis.read(buffer, 0, buffer.length);
            }
        }
    }
}
