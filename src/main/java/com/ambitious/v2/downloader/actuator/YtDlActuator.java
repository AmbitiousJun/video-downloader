package com.ambitious.v2.downloader.actuator;

import cn.hutool.core.util.StrUtil;
import com.ambitious.v2.config.Config;
import com.ambitious.v2.constant.DownloaderType;
import com.ambitious.v2.downloader.actuator.mp4multithread.Mp4MultiThreadActuator;
import com.ambitious.v2.pojo.DownloadMeta;
import com.ambitious.v2.pojo.YtDlDownloadMeta;
import com.ambitious.v2.util.LogUtils;
import com.ambitious.v2.util.M3U8Utils;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.List;

/**
 * 专门处理 youtube-dl 生成的 DownloadMeta 的 Actuator
 * @author ambitious
 * @date 2023/9/16
 */
public class YtDlActuator implements DownloadActuator{

    public static final Logger LOGGER = LoggerFactory.getLogger(YtDlActuator.class);

    private final DownloadActuator mp4Actuator;
    private final DownloadActuator m3u8Actuator;

    public YtDlActuator() {
        if (Config.DOWNLOADER.USE.equals(DownloaderType.MULTI_THREAD)) {
            mp4Actuator = new Mp4MultiThreadActuator();
            m3u8Actuator = new M3U8MultiThreadActuator();
        } else {
            mp4Actuator = new Mp4SimpleActuator();
            m3u8Actuator = new M3U8SimpleActuator();
        }
    }

    @Override
    public void download(DownloadMeta meta) throws Exception {
        // 1 恢复下载信息
        YtDlDownloadMeta ytDlMeta = YtDlDownloadMeta.recoverFromDownloadMeta(meta);
        List<String> links = ytDlMeta.getLinks();
        int size = links.size();
        // 2 拆分任务
        for (int i = 0; i < size; i++) {
            LogUtils.info(LOGGER, String.format("正在处理第 %d / %d 个子任务，文件名：%s", i + 1, size, ytDlMeta.getFileName()));
            String link = links.get(i);
            DownloadMeta tmpMeta = new DownloadMeta(link, ytDlMeta.getFileName().replace(".mp4", getPartSuffix(i)), ytDlMeta.getOriginUrl());
            if (M3U8Utils.checkM3U8(link)) {
                m3u8Actuator.download(tmpMeta);
            } else {
                mp4Actuator.download(tmpMeta);
            }
            LogUtils.success(LOGGER, String.format("第 %d / %d 个子任务处理完成，文件名：%s", i + 1, size, ytDlMeta.getFileName()));
        }
        // 3 合并音视频（如果需要的话）
        mergeSubTask(ytDlMeta, size);
    }

    public void mergeSubTask(YtDlDownloadMeta ytDlMeta, int size) throws Exception {
        LogUtils.info(LOGGER, String.format("正在合并子任务，文件名：%s", ytDlMeta.getFileName()));
        List<String> commands = Lists.newArrayList("ffmpeg");
        for (int i = 0; i < size; i++) {
            commands.add("-i");
            commands.add(ytDlMeta.getFileName().replace(".mp4", getPartSuffix(i)));
        }
        commands.add("-c:v");
        commands.add("copy");
        commands.add("-c:a");
        commands.add("copy");
        commands.add(ytDlMeta.getFileName());
        ProcessBuilder builder = new ProcessBuilder(commands);
        builder.redirectErrorStream(true);
        Process process = builder.start();
        process.waitFor();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line = reader.readLine();
        String invalidInput = "Invalid data found when processing input";
        while (StrUtil.isNotEmpty(line)) {
            if (line.contains(invalidInput)) {
                LogUtils.error(LOGGER, "合并子任务失败，子任务将得到保留，文件名：" + ytDlMeta.getFileName());
                return;
            }
            line = reader.readLine();
        }
        LogUtils.success(LOGGER, "子任务合并完成，正在删除子任务，文件名：" + ytDlMeta.getFileName());
        for (int i = 0; i < size; i++) {
            String partName = ytDlMeta.getFileName().replace(".mp4", getPartSuffix(i));
            File file = new File(partName);
            if (file.exists() && !file.delete()) {
                LogUtils.warning(LOGGER, "子任务删除失败，文件名：" + partName);
            }
        }
        LogUtils.success(LOGGER, "所有子任务已全部删除");
    }

    private String getPartSuffix(int idx) {
        return "_part" + idx + ".mp4";
    }
}
