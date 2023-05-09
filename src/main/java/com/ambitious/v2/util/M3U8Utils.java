package com.ambitious.v2.util;

import cn.hutool.core.util.StrUtil;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Deque;
import java.util.Map;
import java.util.Optional;

/**
 * @author ambitious
 * @date 2023/5/8
 */
public class M3U8Utils {

    private static final Logger LOGGER = LoggerFactory.getLogger(M3U8Utils.class);
    private static final String NETWORK_LINK_PREFIX = "http";
    private static final String LOCAL_FILE_PREFIX = "file";
    private static final TsTransfer TS_TRANSFER;

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
     * 读取 M3U8 文件中的 ts 文件列表
     * @param m3u8Url m3u8 文件的下载地址
     * @param headerMap 请求头，可以为空
     * @return ts 文件列表
     */
    public static Deque<TsMeta> readTsUrls(String m3u8Url, Map<String, String> headerMap) {
        if (m3u8Url.startsWith(NETWORK_LINK_PREFIX)) {
            return readHttpTsUrls(m3u8Url, headerMap);
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
        // 1 找到后缀的位置
        int suffixPos = m3u8Url.indexOf("." + Config.DECODER.RESOURCE_TYPE.value);
        if (suffixPos == -1) {
            throw new RuntimeException("不是规范的 m3u8 文件");
        }
        // 2 找到后缀之前的第一个 '/'
        int lastSepPos = m3u8Url.substring(0, suffixPos).lastIndexOf("/");
        String baseUrl = m3u8Url.substring(0, lastSepPos);
        // 1 读取 m3u8 文件
        try (HttpResponse res = HttpRequest.get(m3u8Url).addHeaders(headerMap).keepAlive(true).execute();
             BufferedReader reader = new BufferedReader(new InputStreamReader(res.bodyStream()))
        ) {
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
        } catch (Exception e) {
            throw new RuntimeException("读取 m3u8 文件失败", e);
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
            throw new RuntimeException("创建临时目录失败，请先删除旧临时目录");
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
