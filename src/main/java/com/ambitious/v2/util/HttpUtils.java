package com.ambitious.v2.util;

import java.io.*;

/**
 * @author ambitious
 * @date 2023/5/8
 */
public class HttpUtils {

    /**
     * 将输入流输出到文件中
     * @param is 输入流
     * @param dest 目标文件
     */
    public static void downloadStream2File(InputStream is, File dest) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(dest);
             BufferedInputStream bis = new BufferedInputStream(is)
        ) {
            byte[] buffer = new byte[1024 * 1024];
            int len = bis.read(buffer, 0, buffer.length);
            while (len > 0) {
                fos.write(buffer, 0, len);
                len = bis.read(buffer, 0, buffer.length);
            }
        }
    }
}
