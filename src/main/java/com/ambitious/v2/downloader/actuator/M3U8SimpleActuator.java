package com.ambitious.v2.downloader.actuator;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.ambitious.v2.config.Config;
import com.ambitious.v2.constant.FileConstant;
import com.ambitious.v2.pojo.DownloadMeta;
import com.ambitious.v2.pojo.TsMeta;
import com.ambitious.v2.util.HttpUtils;
import com.ambitious.v2.util.LogUtils;
import com.ambitious.v2.util.M3U8Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Paths;
import java.util.Deque;
import java.util.regex.Pattern;

/**
 * M3U8 单线程下载器
 * @author ambitious
 * @date 2023/5/8
 */
public class M3U8SimpleActuator extends M3U8Actuator {

    private static final Logger LOGGER = LoggerFactory.getLogger(M3U8SimpleActuator.class);

    @Override
    protected void handleDownload(DownloadMeta meta, Deque<TsMeta> tsMetas, File tempDir) {
        int finish = 0;
        int size = tsMetas.size();
        while (!tsMetas.isEmpty()) {
            TsMeta tsMeta = tsMetas.pop();
            try {
                coreDownload(meta, tsMeta, tempDir);
                LogUtils.info(LOGGER, String.format("请求 ts 文件中，当前进度：%d / %d，目标视频：%s", ++finish, size, meta.getFileName()));
            } catch (Exception e) {
                LogUtils.error(LOGGER, String.format("请求失败，重新加入到队列中，目标视频：%s", meta.getFileName()));
                tsMetas.offerLast(tsMeta);
            }
        }
        LogUtils.success(LOGGER, "ts 列表下载完成");
    }

}
