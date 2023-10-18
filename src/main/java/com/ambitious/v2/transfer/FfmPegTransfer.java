package com.ambitious.v2.transfer;

import cn.hutool.core.util.StrUtil;
import com.ambitious.v2.util.LogUtils;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
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
public class FfmPegTransfer implements TsTransfer {

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
        concatFiles(tsDir, tsPaths, output);
        LogUtils.success(LOGGER, String.format("转码完成，文件名：%s", output.getName()));
    }

    /**
     * 执行 ffmpeg 合并分片
     * @param tsDir ts 文件的目录
     * @param tsPaths 分片路径集合
     * @param output 输出地址
     */
    private void concatFiles(File tsDir, List<String> tsPaths, File output) throws IOException, InterruptedException {
        File tempTsFile = new File(tsDir, String.format("ts_%d.ts", Integer.MAX_VALUE));
        File tempDestFile = new File(output.getAbsolutePath().replace(".mp4", ".ts"));
        if (tempTsFile.exists() && !tempTsFile.delete()) {
            throw new RuntimeException("转码异常");
        }
        if (tempDestFile.exists() && !tempDestFile.delete()) {
            throw new RuntimeException("转码异常");
        }
        // 2 遍历列表合成
        int size = tsPaths.size();
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
                    concatBuilder.append(tsPaths.get(i));
                    continue;
                }
                concatBuilder.append("|").append(tsPaths.get(i));
            }
            current += handleSize;
            String concat = concatBuilder.toString();
            List<String> cmd = Lists.newArrayList(
                    "ffmpeg",
                    "-i",
                    concat,
                    "-c",
                    "copy",
                    tempDestFile.getAbsolutePath()
            );
            executeCmd(cmd);
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
        executeCmd(Lists.newArrayList("ffmpeg", "-i", "concat:" + tempTsFile.getAbsolutePath(), "-c", "copy", output.getAbsolutePath()));
        if (!tempTsFile.delete()) {
            System.out.println("临时 ts 文件删除失败");
        }
    }

    private void executeCmd(List<String> cmd) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line = br.readLine();
        while (StrUtil.isNotEmpty(line)) {
            System.out.println(line);
            line = br.readLine();
        }
    }
}
