package com.ambitious.v2.decoder.ytdl;

import cn.hutool.core.util.StrUtil;
import com.ambitious.v2.config.Config;
import com.ambitious.v2.pojo.YtDlFormatCode;
import com.ambitious.v2.util.LogUtils;
import com.ambitious.v2.util.SleepUtils;
import com.google.common.collect.Lists;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;

/**
 * 当用户没有配置 format code 或者配置的 code 都解析失败时
 * 就让用户自己选择
 * @author ambitious
 * @date 2023/9/26
 */
@AllArgsConstructor
public class YtDlCodeSelector {

    private String url;
    public static final Logger LOGGER = LoggerFactory.getLogger(YtDlCodeSelector.class);
    public static final Pattern RESULT_PATTERN = Pattern.compile("\\[info\\] Available formats for .*");
    public static final String STOP_INPUT = "-1";

    public YtDlFormatCode requestCode() {
        try {
            Scanner scanner = new Scanner(System.in);
            LogUtils.BLOCK_FLAG = Boolean.TRUE;
        out:
            while (true) {
                // 1 执行命令，获取所有可选的 format code
                System.out.println(LogUtils.packMsg(LogUtils.ANSI_WARNING, "正在尝试读取 format code..."));
                if (!executeProcess()) {
                    System.out.println(LogUtils.packMsg(LogUtils.ANSI_DANGER, "执行命令失败，两秒后重试"));
                    SleepUtils.sleep(2000);
                    continue;
                }
                // 2 用户选择
                while (true) {
                    System.out.println(LogUtils.packMsg(LogUtils.ANSI_WARNING, "！！format code 输入规范：[code] 或者 [code1+code2]（不包含[]）"));
                    System.out.println(LogUtils.packMsg(LogUtils.ANSI_WARNING, String.format("！！输入自定义的 format code 进行解析，输入空行可重新读取 code，输入 %s 放弃解析", STOP_INPUT)));
                    System.out.println(LogUtils.packMsg(LogUtils.ANSI_WARNING, "请选择要解析的 format code："));
                    String input = scanner.nextLine();
                    if (StrUtil.isEmpty(input) || StrUtil.isEmpty(input.trim())) {
                        System.out.println(LogUtils.packMsg(LogUtils.ANSI_WARNING, "重新读取 format code..."));
                        continue out;
                    }
                    input = input.trim();
                    if (STOP_INPUT.equals(input)) {
                        throw new RuntimeException("用户放弃手动解析");
                    }
                    // 3 格式校验
                    String[] codes = input.split("\\+");
                    if (codes.length == 1) {
                        return new YtDlFormatCode(input, 1);
                    }
                    if (codes.length == 2) {
                        return new YtDlFormatCode(input, 2);
                    }
                    System.out.println(LogUtils.packMsg(LogUtils.ANSI_DANGER, "输入不合法，请重新输入"));
                }
            }
        } catch (Exception e) {
            System.out.println(LogUtils.packMsg(LogUtils.ANSI_DANGER, "输入异常：" + e.getMessage()));
            throw new RuntimeException(e);
        } finally {
            LogUtils.BLOCK_FLAG = Boolean.FALSE;
        }
    }

    private boolean executeProcess() {
        List<String> commands = Lists.newArrayList(
                "youtube-dl",
                "-F",
                "--no-playlist",
                url
        );
        if (StrUtil.isNotEmpty(Config.DECODER.YOUTUBE_DL.cookiesFrom)) {
            commands.add("--cookies-from-browser");
            commands.add(Config.DECODER.YOUTUBE_DL.cookiesFrom);
        }
        ProcessBuilder pb = new ProcessBuilder(commands);
        pb.redirectErrorStream(true);
        try {
            Process process = pb.start();
            InputStream is = process.getInputStream();
            BufferedReader bf = new BufferedReader(new InputStreamReader(is));
            List<String> results = Lists.newArrayList();
            String line = bf.readLine();
            while (StrUtil.isNotEmpty(line)) {
                results.add(line);
                line = bf.readLine();
            }
            process.waitFor();
            printFormatCode(results);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void printFormatCode(List<String> results) {
        System.out.println(LogUtils.packMsg(LogUtils.ANSI_WARNING, "===== 请手动选择 format code"));
        System.out.println(LogUtils.packMsg(LogUtils.ANSI_WARNING, "===== 解析地址：" + url));
        boolean flag = false;
        final String warning = "WARNING";
        final String error = "ERROR";
        for (String res : results) {
            if (res.startsWith(warning) || res.startsWith(error)) {
                System.out.println(LogUtils.packMsg(LogUtils.ANSI_DANGER, res));
            }
            if (!flag && RESULT_PATTERN.matcher(res).matches()) {
                flag = true;
                continue;
            }
            if (flag) {
                System.out.println(LogUtils.packMsg(LogUtils.ANSI_SUCCESS, res));
            }
        }
        System.out.println(LogUtils.packMsg(LogUtils.ANSI_WARNING, "===== 解析地址：" + url));
    }
}
