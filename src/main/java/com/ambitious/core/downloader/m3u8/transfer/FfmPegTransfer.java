package com.ambitious.core.downloader.m3u8.transfer;

import cn.hutool.core.util.StrUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 通过命令行调用 ffmpeg 来进行转化，
 * 推荐这种方式！
 * 需要先下载 (<a href="https://ffmpeg.org">ffmpeg</a>) 然后将可执行文件配置到系统的环境变量中
 * ts 文件名必须按照格式："ts_%d.ts"
 * @author ambitious
 * @date 2023/5/3
 */
public class FfmPegTransfer implements TsTransfer{

    private final Pattern fileNamePattern;
    private static final Logger LOGGER = LoggerFactory.getLogger(FfmPegTransfer.class);

    public FfmPegTransfer() {
        String regex = "(?<=_)(\\d+)(?=\\.)";
        fileNamePattern = Pattern.compile(regex);
    }

    @Override
    public void ts2Mp4(File tsDir, File output) throws Exception {
        // 1 读取文件并排序
        File[] tsFiles = tsDir.listFiles((dir, name) -> name.endsWith(".ts"));
        if (tsFiles == null || tsFiles.length == 0) {
            return;
        }
        Arrays.sort(tsFiles, (t1, t2) -> {
            Matcher m1 = fileNamePattern.matcher(t1.getName());
            Matcher m2 = fileNamePattern.matcher(t2.getName());
            if (!m1.find() || !m2.find()) {
                throw new RuntimeException("文件名不规范");
            }
            int i1 = Integer.parseInt(m1.group());
            int i2 = Integer.parseInt(m2.group());
            return i1 - i2;
        });
        List<String> tsPaths = Arrays.stream(tsFiles).map(File::getAbsolutePath).collect(Collectors.toList());
        ProcessBuilder pb = new ProcessBuilder("ffmpeg", "-i", "concat:" + String.join("|", tsPaths), "-c", "copy", output.getAbsolutePath());
        pb.redirectErrorStream(true);
        Process p = pb.start();
        BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line = br.readLine();
        while (StrUtil.isNotEmpty(line)) {
            LOGGER.info(line);
            line = br.readLine();
        }
        p.waitFor();
        LOGGER.info("转码完成，文件名：{}", output.getName());
    }
}
