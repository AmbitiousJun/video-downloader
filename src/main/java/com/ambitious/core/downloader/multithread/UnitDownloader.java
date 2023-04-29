package com.ambitious.core.downloader.multithread;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 分片下载器
 * @author ambitious
 * @date 2023/4/29
 */
public class UnitDownloader {

    /**
     * 分片起始字节
     */
    private final int from;
    /**
     * 分片结束字节
     */
    private final int to;
    /**
     * 要下载的文件 URL
     */
    private final String url;
    /**
     * 要将文件保存到本地的哪个具体位置
     */
    private final File dest;

    public UnitDownloader(int from, int to, String url, File dest) {
        this.from = from;
        this.to = to;
        this.url = url;
        this.dest = dest;
    }

    /**
     * 下载分片
     */
    public void download(AtomicInteger fileCurSize) throws Exception {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(this.url).openConnection();
            conn.setRequestProperty("Range", String.format("bytes=%d-%d", this.from, this.to));
            conn.connect();
            InputStream is = conn.getInputStream();
            try (RandomAccessFile file = new RandomAccessFile(this.dest, "rw")) {
                // 定位到文件中该分片的位置
                file.seek(this.from);
                // 缓冲区
                byte[] buffer = new byte[1024 * 1024];
                int len = is.read(buffer, 0, buffer.length);
                while (len > 0) {
                    file.write(buffer, 0, len);
                    // 记录下载的字节数
                    fileCurSize.addAndGet(len);
                    len = is.read(buffer, 0, buffer.length);
                }
            }
        } catch (Exception e) {
            throw new Exception("下载失败");
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}
