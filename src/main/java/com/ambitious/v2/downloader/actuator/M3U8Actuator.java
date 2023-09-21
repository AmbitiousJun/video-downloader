package com.ambitious.v2.downloader.actuator;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import com.ambitious.v2.constant.FileConstant;
import com.ambitious.v2.pojo.DownloadMeta;
import com.ambitious.v2.pojo.TsMeta;
import com.ambitious.v2.util.HttpUtils;
import com.ambitious.v2.util.LogUtils;
import com.ambitious.v2.util.M3U8Utils;
import com.ambitious.v2.util.SleepUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Deque;

/**
 * 提取 m3u8 下载的公共逻辑（模板方法模式）
 * @author ambitious
 * @date 2023/5/9
 */
public abstract class M3U8Actuator implements DownloadActuator {

    private static final Logger LOGGER = LoggerFactory.getLogger(M3U8Actuator.class);

    @Override
    public void download(DownloadMeta meta) throws Exception {
        // 读取 ts 文件
        LogUtils.info(LOGGER, String.format("正在读取 ts 文件列表, 目标视频：%s", meta.getFileName()));
        Deque<TsMeta> tsMetas = M3U8Utils.readTsUrls(meta.getLink(), meta.getHeaderMap());
        int size = tsMetas.size();
        LogUtils.success(LOGGER, String.format("读取完成，共发现 %d 个 ts 文件，目标视频：%s", size, meta.getFileName()));
        if (size == 0) {
            LogUtils.warning(LOGGER, String.format("这是一个空的 m3u8 文件，下载取消，目标视频：%s", meta.getFileName()));
            return;
        }
        // 初始化临时文件夹
        File tempDir = M3U8Utils.initTempDir(meta.getFileName());
        // 执行下载
        this.handleDownload(meta, tsMetas, tempDir);
        // 合并文件
        M3U8Utils.merge(tempDir);
        LogUtils.success(LOGGER, String.format("视频下载完成，目标视频：%s", meta.getFileName()));
    }

    /**
     * 核心的下载逻辑，由子类实现
     * @param meta 下载信息
     * @param tsMetas ts urls
     * @param tempDir 存储 ts 文件的临时目录
     * @throws InterruptedException 睡眠中断异常
     */
    protected abstract void handleDownload(DownloadMeta meta, Deque<TsMeta> tsMetas, File tempDir) throws InterruptedException;

    protected void coreDownload(DownloadMeta meta, TsMeta tsMeta, File tempDir) throws Exception {
        File ts = new File(tempDir, String.format(FileConstant.TS_FILENAME_FORMAT, tsMeta.getIndex()));
        if (ts.exists()) {
            LogUtils.warning(LOGGER, "分片已存在，跳过下载");
            return;
        }
        boolean success = false;
        while (!success) {
            HttpURLConnection conn = null;
            try {
                conn = (HttpURLConnection) new URL(tsMeta.getUrl()).openConnection();
                conn.setRequestMethod("GET");
                if (CollectionUtil.isNotEmpty(meta.getHeaderMap())) {
                    for (String key : meta.getHeaderMap().keySet()) {
                        conn.setRequestProperty(key, meta.getHeaderMap().get(key));
                    }
                }
                InputStream is = conn.getInputStream();
                int code = conn.getResponseCode();
                if (code == HttpStatus.HTTP_MOVED_TEMP || code == HttpStatus.HTTP_MOVED_PERM) {
                    HttpUtil.downloadFile(conn.getHeaderField("Location"), ts);
                } else if (code != HttpStatus.HTTP_OK) {
                    LogUtils.warning(LOGGER, "分片下载失败，两秒后重试");
                    SleepUtils.sleep(2000);
                    continue;
                }
                HttpUtils.downloadStream2File(is, ts);
                success = true;
            } catch (Exception e) {
                LogUtils.warning(LOGGER, "分片下载失败，两秒后重试");
                SleepUtils.sleep(200);
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }
    }
}
