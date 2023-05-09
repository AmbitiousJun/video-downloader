package com.ambitious.v2.transfer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;

/**
 * 使用 Java 自带的 FileChannel 进行转化
 * ！注：这种方式只是简单地将文件拼接在一起，有可能会导致文件异常
 * @author ambitious
 * @date 2023/5/3
 */
public class FileChannelTsTransfer implements TsTransfer {

    @Override
    public void ts2Mp4(File tsDir, File output) throws Exception {
        try (FileOutputStream fos = new FileOutputStream(output)) {
            FileChannel outChannel = fos.getChannel();
            File[] tsFiles = tsDir.listFiles((dir, name) -> name.endsWith(".ts"));

            if (tsFiles == null || tsFiles.length == 0) {
                return;
            }

            for (File ts : tsFiles) {
                try (FileInputStream fis = new FileInputStream(ts)) {
                    FileChannel inChannel = fis.getChannel();
                    inChannel.transferTo(0, inChannel.size(), outChannel);
                }
            }
        }
    }
}
