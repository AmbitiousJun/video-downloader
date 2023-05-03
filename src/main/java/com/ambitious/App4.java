package com.ambitious;

import com.ambitious.core.downloader.m3u8.transfer.CvTsTransfer;
import com.ambitious.core.downloader.m3u8.transfer.FfmPegTransfer;
import com.ambitious.core.downloader.m3u8.transfer.TsTransfer;

import java.io.File;

/**
 * @author ambitious
 * @date 2023/5/3
 */
public class App4 {

    public static void main(String[] args) throws Exception {
        File tmpDir = new File("/Users/ambitious/Downloads/0112023-05-03 第3集：一名惊人（下）_ts_tmp_files");
        File output = new File("/Users/ambitious/Downloads/0112023-05-03 第3集：一名惊人（下）.mp4");

        TsTransfer transfer = new FfmPegTransfer();
        transfer.ts2Mp4(tmpDir, output);
    }
}
