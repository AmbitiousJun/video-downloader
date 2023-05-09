package com.ambitious.v1.downloader.m3u8.transfer;

import com.google.common.collect.Lists;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;

import java.io.File;
import java.util.List;
import java.util.Optional;

/**
 * 使用 JavaCV 库进行合并转化 ts 文件
 * 这个库内置了 ffmpeg，可以在系统没有安装 ffmpeg 的情况下进行转码，
 * 但是在当前版本中，转码出来的视频不仅大、而且糊，不建议使用
 * @author ambitious
 * @date 2023/5/3
 */
public class CvTsTransfer implements TsTransfer {

    @Override
    public void ts2Mp4(File tsDir, File output) throws Exception {
        // 1 获取 ts 文件列表
        List<File> tsFiles = Lists.newArrayList();
        int length = Optional.ofNullable(tsDir.listFiles((dir, name) -> name.endsWith(".ts"))).orElse(new File[0]).length;
        for (int i = 1; i <= length; i++) {
            tsFiles.add(new File(tsDir, "ts_" + i + ".ts"));
        }
        if (tsFiles.size() == 0) {
            return;
        }

        // 2 创建 Grabber（输入）
        FFmpegFrameGrabber grabber = getGrabber(tsFiles.get(0));
        // 3 创建 Recorder（输出）
        FFmpegFrameRecorder recorder = getRecorder(output, grabber);
        try {
            for (File ts : tsFiles) {
                FFmpegFrameGrabber g = getGrabber(ts);
                try {
                    Frame frame = g.grabFrame();
                    while (frame != null) {
                        recorder.record(frame);
                        frame = g.grabFrame();
                    }
                } finally {
                    g.stop();
                    g.close();
                }
            }
        } finally {
            grabber.stop();
            grabber.close();
            recorder.stop();
            recorder.close();
        }

    }

    private FFmpegFrameGrabber getGrabber(File tsFile) throws FFmpegFrameGrabber.Exception {
        FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(tsFile);
        grabber.start();
        return grabber;
    }

    private FFmpegFrameRecorder getRecorder(File output, FFmpegFrameGrabber grabber) throws FFmpegFrameRecorder.Exception {
        FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(output, grabber.getImageWidth(), grabber.getImageHeight(), grabber.getAudioChannels());
        recorder.setFrameRate(grabber.getFrameRate());

        recorder.setVideoCodec(grabber.getVideoCodec());
        recorder.setVideoQuality(0);
        recorder.setVideoBitrate(grabber.getVideoBitrate());

        recorder.setAspectRatio(grabber.getAspectRatio());

        recorder.setAudioBitrate(grabber.getAudioBitrate());
        recorder.setAudioQuality(0);
        recorder.setAudioCodec(grabber.getAudioCodec());

        recorder.setFormat(grabber.getFormat());

        recorder.start();
        return recorder;
    }
}
