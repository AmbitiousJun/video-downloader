package com.ambitious.v2.util;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.ambitious.v2.config.Config;
import com.google.common.collect.Maps;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author ambitious
 * @date 2023/5/8
 */
public class HttpUtils {


    public static final Logger LOGGER = LoggerFactory.getLogger(HttpUtils.class);

    /**
     * 建立连接的超时时间
     */
    public static final Integer CONNECT_TIMEOUT = 60 * 1000;

    /**
     * 读取数据的超时时间
     */
    public static final Integer READ_TIMEOUT = 300 * 1000;

    /**
     * 用于匹配出 Http 请求头中 Ranges 的值
     */
    public static final Pattern HTTP_HEADER_RANGES_PATTERN = Pattern.compile("bytes=(\\d*)-(\\d*)");

    /**
     * Range 请求头 key
     */
    public static final String HTTP_HEADER_RANGES_KEY = "Range";

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
    @SuppressWarnings("all")
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
     * 具备超时控制的 okHttpClient
     */
    private static class MyOkHttpClientHolder {
        static final OkHttpClient CLIENT = new OkHttpClient.Builder()
                                              .callTimeout(HttpUtils.READ_TIMEOUT, TimeUnit.MILLISECONDS)
                                              .readTimeout(HttpUtils.READ_TIMEOUT, TimeUnit.MILLISECONDS)
                                              .build();
    }

    public static OkHttpClient getOkHttpClient() {
        return MyOkHttpClientHolder.CLIENT;
    }

    /**
     * 下载一个网络资源到本地的文件上，并进行网络限速
     * @param request 构造好的 okhttp 请求对象
     * @param dest 要下载到本地的哪一个文件上
     * @throws Exception 下载过程中可能发生的任何异常
     */
    public static void downloadWithRateLimit(Request request, File dest) throws Exception {
        String url = request.url().url().toString();
        String method = request.method();
        Map<String, String> headers = multiMap2HashMap(request.headers().toMultimap());
        // 1 预请求，获取要下载文件的总大小
        long[] ranges = getRequestRanges(url, method, headers);
        removeRangeHeader(headers);
        long start = ranges[0];
        long end = ranges[1];
        // 2 分割文件进行下载，每次下载前先到令牌桶中获取能够下载的字节数
        MyTokenBucket bucket = Config.DOWNLOADER.TOKEN_BUCKET;
        OkHttpClient client = getOkHttpClient();
        try (RandomAccessFile file = new RandomAccessFile(dest, "rw")) {
            while (start < end) {
                long consume = bucket.tryConsume(end - start);
                if (consume <= 0) {
                    // 抢不到令牌，睡眠一小会，防止过度消耗系统资源
                    SleepUtils.sleep(100);
                    continue;
                }
                String rangeHeader = String.format("bytes=%s-%s", start, start + consume);
                // 请求部分资源
                Request subRequest = new Request.Builder(request)
                        .header(HTTP_HEADER_RANGES_KEY, rangeHeader)
                        .build();
                try (Response resp = client.newCall(subRequest).execute()) {
                    int code = resp.code();
                    if (!is2xxSuccess(code)) {
                        throw new IOException("错误码：" + code);
                    }
                    ResponseBody body = resp.body();
                    if (body == null) {
                        throw new IOException("响应体为空");
                    }
                    // 3 将请求下来的文件分片，使用 RandomAccessFile 写入到目的文件中
                    file.seek(start);
                    file.write(body.bytes());
                    start += consume;
                    bucket.completeConsume(consume);
                } catch (Exception e) {
                    LogUtils.warning(LOGGER, String.format("分片下载异常：%s，两秒后重试", e.getMessage()));
                    SleepUtils.sleep(2000);
                }
            }
        }
    }

    /**
     * 下载文件时，可以添加 Range 请求头来请求文件的部分字节
     * 本方法返回的是要请求的 url 的字节范围
     * 如果 headers 中已经存在 Range 头，直接返回
     * 否则发送 http 请求获取 contentLength，返回值是 [0, contentLength]
     * @param url 要请求的目的 url
     * @param method 请求方法
     * @param headers 请求头
     * @return 字节范围
     */
    public static long[] getRequestRanges(String url, String method, Map<String, String> headers) throws Exception {
        if (StrUtil.isEmpty(url) || headers == null) {
            throw new IllegalArgumentException("url 和 headers 必传");
        }
        method = Optional.ofNullable(method).orElse("GET");
        String range = headers.getOrDefault(HTTP_HEADER_RANGES_KEY, null);
        range = headers.getOrDefault(HTTP_HEADER_RANGES_KEY.toLowerCase(), range);
        if (StrUtil.isEmpty(range)) {
            // 请求头中没有 Range，就返回 [0, contentLength]
            return getRequestRanges(url, method, headers, 0);
        }
        Matcher m = HTTP_HEADER_RANGES_PATTERN.matcher(range);
        if (!m.find()) {
            // 请求头不合法，忽略
            return getRequestRanges(url, method, headers, 0);
        }
        String from = m.group(1);
        String to = m.group(2);
        // from to 全空，是无效的 Range 头，直接去除
        if (StrUtil.isAllEmpty(from, to)) {
            removeRangeHeader(headers);
            return getRequestRanges(url, method, headers);
        }
        // 有 from 没 to，to 直接取 Content-Length
        if (StrUtil.isNotEmpty(from) && StrUtil.isEmpty(to)) {
            return getRequestRanges(url, method, headers, Long.parseLong(from));
        }
        // 两者都有，直接返回
        from = StrUtil.isEmpty(from) ? "0" : from;
        return new long[] { Long.parseLong(from), Long.parseLong(to) };
    }

    /**
     * 移除 Range 请求头
     * @param headers 要移除的 map 对象
     */
    public static void removeRangeHeader(Map<String, String> headers) {
        headers.remove(HTTP_HEADER_RANGES_KEY);
        headers.remove(HTTP_HEADER_RANGES_KEY.toLowerCase());
    }

    /**
     * 下载文件时，可以添加 Range 请求头来请求文件的部分字节
     * 本方法返回的是要请求的 url 的字节范围
     * 发送 http 请求获取 contentLength，返回值是 [from, contentLength]
     * @param url 要请求的目的 url
     * @param method 请求方法
     * @param headers 请求头
     * @param from 作为返回值数组中的第一个值
     * @return 字节范围
     */
    public static long[] getRequestRanges(String url, String method, Map<String, String> headers, long from) throws Exception {
        if (StrUtil.isEmpty(url) || headers == null) {
            throw new IllegalArgumentException("url 和 headers 必传");
        }
        removeRangeHeader(headers);
        HttpURLConnection conn = genHttpConnection(new HttpOptions(url, method, headers));
        try {
            conn.setRequestMethod("HEAD");
            conn.connect();
            int code = conn.getResponseCode();
            if (!is2xxSuccess(code)) {
                throw new IOException("连接远程 url 失败");
            }
            long contentLength = conn.getContentLengthLong();
            if (contentLength == -1) {
                throw new IOException("无法获取资源的 Content-Length 属性");
            }
            return new long[] { from, contentLength };
        } finally {
            closeConn(conn);
        }
    }

    /**
     * 判断一个 http 请求的响应码是否是 2xx 类型的成功码
     * @param code 响应码
     * @return 是否 2xx
     */
    public static boolean is2xxSuccess(int code) {
        String codeStr = String.valueOf(code);
        return codeStr.startsWith("2");
    }

    /**
     * 将 okhttp 内部的 MultiMap 转换为普通的 HashMap
     * @param multiMap okhttp 内部的 MultiMap
     * @return 普通的 HashMap
     */
    public static Map<String, String> multiMap2HashMap(Map<String, List<String>> multiMap) {
        Map<String, String> res = Maps.newHashMap();
        for (String key : multiMap.keySet()) {
            List<String> values = multiMap.get(key);
            for (String value : values) {
                res.put(key, value);
            }
        }
        return res;
    }

    /**
     * 将输入流输出到文件中
     * @param is 输入流
     * @param dest 目标文件
     */
    public static void downloadStream2File(InputStream is, File dest, long contentLength) throws IOException {
        LogUtils.info(LOGGER, String.format("正在下载流，大小：%.2f MB, 文件名：%s", Long.valueOf(contentLength).doubleValue() / 1024 / 1024, dest.getName()));
        try (BufferedOutputStream bos = new BufferedOutputStream(Files.newOutputStream(dest.toPath()));
             BufferedInputStream bis = new BufferedInputStream(is)
        ) {
            // 缓冲区大小不能超过下载器的速率限制
            byte[] buffer = new byte[1024 * 1024];
            // 获取当前能够读取的字节数
            int len = bis.read(buffer, 0, buffer.length);
            while (len > 0) {
                bos.write(buffer, 0, len);
                len = bis.read(buffer, 0, buffer.length);
            }
        }
    }
}
