package com.ambitious.v2.util;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.ambitious.v2.config.Config;
import com.ambitious.v2.pojo.TsMeta;
import com.ambitious.v2.transfer.CvTsTransfer;
import com.ambitious.v2.transfer.FfmPegTransfer;
import com.ambitious.v2.transfer.FileChannelTsTransfer;
import com.ambitious.v2.transfer.TsTransfer;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

/**
 * @author ambitious
 * @date 2023/5/8
 */
public class M3U8Utils {

    private static final Logger LOGGER = LoggerFactory.getLogger(M3U8Utils.class);
    private static final String NETWORK_LINK_PREFIX = "http";
    private static final String LOCAL_FILE_PREFIX = "file";
    private static final TsTransfer TS_TRANSFER;
    private static final Set<String> VALID_M3U8_CONTENT_TYPES = Sets.newHashSet(
            "application/vnd.apple.mpegurl",
            "application/x-mpegurl"
    );

    static {
        switch (Config.TRANSFER.getUSE()) {
            case FILE_CHANNEL:
                TS_TRANSFER = new FileChannelTsTransfer();
                break;
            case CV:
                TS_TRANSFER = new CvTsTransfer();
                break;
            case FFMPEG:
                TS_TRANSFER = new FfmPegTransfer();
                break;
            default:
                throw new RuntimeException("ts 转换器配置错误");
        }
    }

    /**
     * 检查一个 url 是否是一个 M3U8 链接，可以是网络链接也可以是本地文件
     * @param url 要检查的链接
     * @param headerMap 请求头
     * @return 检查是否通过
     */
    public static boolean checkM3U8(String url, Map<String, String> headerMap) {
        if (StrUtil.isEmpty(url)) {
            return false;
        }
        while (true) {
            LogUtils.info(LOGGER, "正在解析 m3u8 信息...");
            HttpURLConnection conn = null;
            try {
                conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Connection", "Close");
                if (CollectionUtil.isNotEmpty(headerMap)) {
                    for (String key : headerMap.keySet()) {
                        conn.setRequestProperty(key, headerMap.get(key));
                    }
                }
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                int code = conn.getResponseCode();
                if (code != 200) {
                    throw new RuntimeException();
                }
                String contentType = conn.getHeaderField("Content-Type");
                if (StrUtil.isEmpty(contentType)) {
                    throw new RuntimeException();
                }
                contentType = contentType.toLowerCase().split(";")[0];
                if (!VALID_M3U8_CONTENT_TYPES.contains(contentType)) {
                    return false;
                }
                String line = reader.readLine();
                if (StrUtil.isEmpty(line)) {
                    return false;
                }
                String valid = "#EXTM3U";
                line = line.substring(0, Math.min(valid.length(), line.length()));
                return line.equalsIgnoreCase(valid);
            } catch (Exception e) {
                LogUtils.warning(LOGGER, String.format("解析异常：%s，两秒后重试", e.getMessage()));
                SleepUtils.sleep(2000);
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }
    }

    /**
     * 读取 M3U8 文件中的 ts 文件列表
     * @param m3u8Url m3u8 文件的下载地址
     * @param headerMap 请求头，可以为空
     * @return ts 文件列表
     */
    public static Deque<TsMeta> readTsUrls(String m3u8Url, Map<String, String> headerMap) throws InterruptedException {
        if (m3u8Url.startsWith(NETWORK_LINK_PREFIX)) {
            return readHttpTsUrls(m3u8Url, headerMap);
        }
        // 解析器使用的是 nio 写出文件，这里使用自旋锁等待文件生成
        File file = new File(m3u8Url.substring(7));
        while (!file.exists()) {
            LOGGER.info("查找不到本地的 m3u8 文件：{}", m3u8Url);
            SleepUtils.sleep(1000);
        }
        // 1 读取 m3u8 文件
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new URL(m3u8Url).openStream()))) {
            Deque<TsMeta> ans = Queues.newArrayDeque();
            // 2 读取数据，封装对象
            String line = reader.readLine();
            while (line != null) {
                if (line.startsWith("#") || StrUtil.isBlank(line)) {
                    // 去除注释和空行
                    line = reader.readLine();
                    continue;
                }
                String metaUrl = line;
                if (!line.startsWith(NETWORK_LINK_PREFIX)) {
                    throw new RuntimeException("m3u8 文件数据不完整");
                }
                ans.offerLast(new TsMeta(metaUrl, ans.size() + 1));
                line = reader.readLine();
            }
            return ans;
        } catch (Exception e) {
            throw new RuntimeException("读取 m3u8 文件失败", e);
        } finally {
            File localM3U8 = new File(m3u8Url.substring(LOCAL_FILE_PREFIX.length() + 3));
            if (localM3U8.exists() && !localM3U8.delete()) {
                LOGGER.error("本地 m3u8 文件删除失败");
            }
        }
    }

    /**
     * 读取网络 M3U8 文件
     * @param m3u8Url url
     * @param headerMap 请求头
     * @return ts urls
     */
    private static Deque<TsMeta> readHttpTsUrls(String m3u8Url, Map<String, String> headerMap) {
        if (!checkM3U8(m3u8Url, headerMap)) {
            throw new RuntimeException("不是规范的 m3u8 文件");
        }
        // 1 找到后缀的位置
        int queryPos = m3u8Url.indexOf("?");
        queryPos = queryPos == -1 ? m3u8Url.length() : queryPos;
        // 2 找到后缀之前的第一个 '/'
        int lastSepPos = m3u8Url.substring(0, queryPos).lastIndexOf("/");
        String baseUrl = m3u8Url.substring(0, lastSepPos);
        // 1 读取 m3u8 文件
        while (true) {
            try (HttpResponse res = HttpRequest.get(m3u8Url).addHeaders(headerMap).keepAlive(true).execute();
            ) {
                if (res.getStatus() != 200) {
                    LOGGER.info("请求 m3u8 文件失败：触发频繁请求，两秒后重试");
                    SleepUtils.sleep(2000);
                    continue;
                }
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(res.bodyStream()))) {
                    Deque<TsMeta> ans = Queues.newArrayDeque();
                    // 2 读取数据，封装对象
                    String line = reader.readLine();
                    while (line != null) {
                        if (line.startsWith("#") || StrUtil.isBlank(line)) {
                            // 去除注释和空行
                            line = reader.readLine();
                            continue;
                        }
                        String metaUrl = line;
                        if (!line.startsWith(NETWORK_LINK_PREFIX)) {
                            metaUrl = baseUrl + "/" + metaUrl;
                        }
                        ans.offerLast(new TsMeta(metaUrl, ans.size() + 1));
                        line = reader.readLine();
                    }
                    return ans;
                }
            } catch (Exception e) {
                throw new RuntimeException("读取 m3u8 文件失败", e);
            }
        }
    }

    /**
     * 生成一个用于下载 ts 文件的临时目录
     * @param filename 文件名称
     * @return 临时目录对象
     */
    public static File initTempDir(String filename) {
        File tempDir = new File(filename + "_" + Config.DOWNLOADER.TS_DIR_SUFFIX);
        if (tempDir.exists() && !tempDir.delete()) {
            LogUtils.warning(LOGGER, "临时目录已存在：" + tempDir.getName());
            return tempDir;
        }
        if (!tempDir.mkdirs()) {
            throw new RuntimeException("创建临时目录失败");
        }
        return tempDir;
    }

    /**
     * 合并 ts 文件列表
     * @param tempDir 临时目录
     */
    public static void merge(File tempDir) throws Exception {
        String fileName = tempDir.getName().substring(0, tempDir.getName().length() - Config.DOWNLOADER.TS_DIR_SUFFIX.length() - 1);
        LOGGER.info("准备将 ts 文件合并成 mp4 文件，目标视频: {}", fileName);
        TS_TRANSFER.ts2Mp4(tempDir, new File(tempDir.getParentFile(), fileName));
        LOGGER.info("合并完成，将临时 ts 目录删除，目标视频：{}", fileName);
        if (!deleteTempDir(tempDir)) {
            LOGGER.error("临时目录删除失败，目标视频：{}", fileName);
        }
    }

    /**
     * 删除临时目录
     * @param tempDir 临时目录
     * @return 是否删除成功
     */
    public static boolean deleteTempDir(File tempDir) {
        // 1 删除子文件
        for (File file : Optional.ofNullable(tempDir.listFiles()).orElse(new File[0])) {
            if (!file.delete()) {
                return false;
            }
        }
        // 2 删除目录
        return tempDir.delete();
    }
}
