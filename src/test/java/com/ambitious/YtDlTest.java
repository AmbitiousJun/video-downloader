package com.ambitious;

import cn.hutool.core.util.StrUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * 测试 youtube-dl
 * @author ambitious
 * @date 2023/9/16
 */
public class YtDlTest {

    public static void main(String[] args) throws Exception {
        ProcessBuilder builder = new ProcessBuilder("youtube-dl", "-f", "3", "--get-url" ,"--cookies-from-browser", "chrome" ,"https://v.youku.com/v_show/id_XMTUxODgzNTU0MA==.html?spm=a2h0c.8166622.PhoneSokuProgram_1.dselectbutton_1&showid=f7c3e8ccaeee11e5b692");
        builder.redirectErrorStream(true);
        Process process = builder.start();
        process.waitFor();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line = reader.readLine();
        while (StrUtil.isNotEmpty(line)) {
            System.out.println(line);
            line = reader.readLine();
        }
    }
}
