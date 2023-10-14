package com.ambitious.v2.decoder.ytdl;

import cn.hutool.core.util.StrUtil;
import com.ambitious.v2.config.Config;
import com.ambitious.v2.pojo.YtDlFormatCode;
import com.google.common.collect.Lists;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

/**
 * 负责 youtube-dl 的一次执行过程
 * @author ambitious
 * @date 2023/9/16
 */
public class YtDlHandler {

    private final Process process;
    private final Integer expectedUrlNums;

    public YtDlHandler(String url, YtDlFormatCode formatCode) throws IOException {
        this.expectedUrlNums = formatCode.getExpectedLinkNums();
        List<String> commands = Lists.newArrayList(
  "youtube-dl",
            "-f", formatCode.getCode(),
            url,
            "--get-url",
            "--no-playlist"
        );
        if (StrUtil.isNotEmpty(Config.DECODER.YOUTUBE_DL.cookiesFrom)) {
            commands.add("--cookies-from-browser");
            commands.add(Config.DECODER.YOUTUBE_DL.cookiesFrom);
        }
        ProcessBuilder builder = new ProcessBuilder(commands);
        builder.redirectErrorStream(true);
        this.process = builder.start();
    }

    /**
     * 获取下载链接
     * @return 链接列表
     */
    public List<String> getLinks() throws InterruptedException, IOException {
        process.waitFor();
        List<String> links = Lists.newArrayList();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line = reader.readLine();
        while (StrUtil.isNotEmpty(line)) {
            if (line.startsWith("http")) {
                links.add(line);
            }
            line = reader.readLine();
        }
        if (!expectedUrlNums.equals(links.size())) {
            throw new RuntimeException("解析链接失败，与预期链接数不符");
        }
        return links;
    }
}
