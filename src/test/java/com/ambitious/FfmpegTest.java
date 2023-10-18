package com.ambitious;

import cn.hutool.core.util.StrUtil;
import com.google.common.collect.Lists;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Ambitious
 * @date 2023/10/18
 */
public class FfmpegTest {

    public static final Pattern FILE_NAME_PATTERN = Pattern.compile("(?<=_)(\\d+)(?=\\.)");

    /**
     * ts 文件存放路径
     */
    public static final String tsDirPath = "C:/Users/Ambitious/Downloads/test";
    /**
     * 最终合成视频的存放位置
     */
    public static final String outputFilePath = "C:/Users/Ambitious/Downloads/测试.mp4";

    public static void main(String[] args) throws IOException {
        // 1 读取 ts 文件列表
        File tsDir = new File(tsDirPath);
        // 这个时候获取到的文件列表还不包含临时 ts
        File[] files = tsDir.listFiles();
        assert files != null;
        Arrays.sort(files, (t1, t2) -> {
            Matcher m1 = FILE_NAME_PATTERN.matcher(t1.getName());
            Matcher m2 = FILE_NAME_PATTERN.matcher(t2.getName());
            if (!m1.find() || !m2.find()) {
                throw new RuntimeException("文件名不规范");
            }
            int i1 = Integer.parseInt(m1.group());
            int i2 = Integer.parseInt(m2.group());
            return i1 - i2;
        });
        File tempTsFile = new File(tsDir, String.format("ts_%d.ts", Integer.MAX_VALUE));
        File tempDestFile = new File(outputFilePath.replace(".mp4", ".ts"));
        File output = new File(outputFilePath);
        // TODO 清空文件
        // 2 遍历列表合成
        int size = files.length;
        int current = 0;
        while (current < size) {
            // 一次性合并 50 个分片
            int handleSize = Math.min(50, size - current);
            StringBuilder concatBuilder = new StringBuilder("concat:");
            if (tempTsFile.exists()) {
                concatBuilder.append(tempTsFile.getAbsolutePath());
            }
            for (int i = 0; i < handleSize; i++) {
                if (i == 0 && !tempTsFile.exists()) {
                    // 首次合并，不需要 |
                    concatBuilder.append(files[i].getAbsolutePath());
                    continue;
                }
                concatBuilder.append("|").append(files[i].getAbsolutePath());
            }
            current += handleSize;
            String concat = concatBuilder.toString();
            List<String> cmds = Lists.newArrayList(
                    "ffmpeg",
                    "-i",
                    concat,
                    "-c",
                    "copy",
                    tempDestFile.getAbsolutePath()
            );
            ProcessBuilder builder = new ProcessBuilder(cmds);
            builder.redirectErrorStream(true);
            Process process = builder.start();
            BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = br.readLine();
            while (StrUtil.isNotEmpty(line)) {
                System.out.println(line);
                line = br.readLine();
            }
            if (tempTsFile.exists() && !tempTsFile.delete()) {
                throw new RuntimeException("转码异常");
            }
            // 将生成好的文件移动到当前文件夹里面
            if (!tempDestFile.exists() || !tempDestFile.renameTo(tempTsFile)) {
                throw new RuntimeException("转码异常");
            }
        }
        // 全部转换完成后，生成最终文件
        if (!tempTsFile.exists()) {
            throw new RuntimeException("转码异常");
        }
        ProcessBuilder pb = new ProcessBuilder("ffmpeg", "-i", "concat:" + tempTsFile.getAbsolutePath(), "-c", "copy", output.getAbsolutePath());
        pb.redirectErrorStream(true);
        Process process = pb.start();
        BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line = br.readLine();
        while (StrUtil.isNotEmpty(line)) {
            System.out.println(line);
            line = br.readLine();
        }
        if (!tempTsFile.delete()) {
            System.out.println("临时 ts 文件删除失败");
        }
    }
}
