package com.ambitious.test;

import com.ambitious.v1.downloader.MultiThreadManager;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 测试将零散的抓包文件中获取到 ts 请求路径并合成一个 m3u8 文件
 * @author ambitious
 * @date 2023/5/1
 */
public class App3 {

    public static final String BASE_URL = "http://baiducdncnc.inter.iqiyi.com";
    public static final String ORIGIN = "/Users/ambitious/临时文件";
    public static final String DOWNLOAD_URL = "/Users/ambitious/Downloads";
    private static final Logger LOGGER = LoggerFactory.getLogger(App3.class);

    public static String readFileAsString(String url) throws IOException {
        return new String(Files.readAllBytes(Paths.get(url)));
    }

    public static void writeStringAsFile(String str, File file) throws IOException {
        file.getParentFile().mkdirs();
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
            bw.write(str, 0, str.length());
        }
    }

    public static List<String> regexText(String text, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text);
        List<String> res = Lists.newArrayList();
        while (matcher.find()) {
            res.add(matcher.group());
        }
        return res;
    }

    @Data
    @AllArgsConstructor
    static class FileUnit {
        int from;
        int to;
        File dest;
        String url;
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        // 1 解析原抓包文件
        String text = readFileAsString(ORIGIN + "/古宅风云1.m3u8");
        String urlRegex = "(?<=GET ).*?(?= HTTP/1\\.1)";
        String hostRegex = "(?<=Host: ).*?(?=\\\\r\\\\n)";
        String rangeRegex = "(?<=Range: ).*?(?=\\\\r\\\\n)";
        List<String> urls = regexText(text, urlRegex);
        List<String> hosts = regexText(text, hostRegex);
        List<String> ranges = regexText(text, rangeRegex);
        Set<String> filterSet = Sets.newHashSet();
        for (int i = 0; i < ranges.size(); i++) {
            filterSet.add("http://" + hosts.get(i) + urls.get(i) + ";" + ranges.get(i));
        }
        File dest = new File(DOWNLOAD_URL + "/2020-08-06.古宅风云1.mp4");
        // 2 将解析结果封装成对象
        // 3 将分片列表按照 Range 从小到大进行排序
        List<FileUnit> units = filterSet.stream().map(link -> {
            String[] arr = link.split(";");
            String range = arr[1];
            int sepIdx = range.indexOf('-');
            int from = Integer.parseInt(range.substring(6, sepIdx));
            int to = Integer.parseInt(range.substring(sepIdx + 1));
            return new FileUnit(from, to, dest, arr[0]);
        }).sorted(Comparator.comparingInt(o -> o.from)).collect(Collectors.toList());
        // 4 下载
        // AtomicInteger size = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(units.size());

        for (int i = 0; i < units.size(); i++) {
            FileUnit unit = units.get(i);
            File file = new File(DOWNLOAD_URL + "/ts_" + i + ".ts");
            file.createNewFile();
            MultiThreadManager.exec(() -> {
                try {
                    HttpURLConnection conn = (HttpURLConnection) new URL(unit.getUrl()).openConnection();
                    conn.setRequestProperty("Range", String.format("bytes=%d-%d", unit.getFrom(), unit.getTo()));
                    conn.connect();
                    InputStream is = conn.getInputStream();
                    try (BufferedOutputStream bf = new BufferedOutputStream(Files.newOutputStream(file.toPath()))) {
                        byte[] buffer = new byte[1024];
                        int len = is.read(buffer, 0, buffer.length);
                        while (len > 0) {
                            bf.write(buffer, 0, len);
                            len = is.read(buffer, 0, buffer.length);
                        }
                    }
                    conn.disconnect();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        FileChannel outChannel = new FileOutputStream(DOWNLOAD_URL + "/测试.mp4").getChannel();
        for (int i = 0; i < units.size(); i++) {
            FileChannel inChannel = new FileInputStream(DOWNLOAD_URL + "/ts_" + i + ".ts").getChannel();
            inChannel.transferTo(0, inChannel.size(), outChannel);
            inChannel.close();
        }
        outChannel.close();

        // for (FileUnit unit : units) {
        //     MultiThreadManager.exec(() -> {
        //         try {
        //             new UnitDownloader(unit.getFrom(), unit.getTo(), unit.getUrl(), unit.getDest()).download(size);
        //         } catch (Exception e) {
        //             // 下载失败，重新加到下载列表
        //             throw new RuntimeException(e);
        //         } finally {
        //             latch.countDown();
        //             LOGGER.info("剩余：{}", latch.getCount());
        //         }
        //     });
        // }
        latch.await();
        System.exit(0);

        // writeStringAsFile(sb.toString(), new File(ORIGIN + "/破事精英/破事精英.S01E01.m3u8"));
    }

    private static void download2(Set<String> filterSet) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL("http://122.205.109.41/r/baiducdncnc.inter.iqiyi.com/videos/vts/20220920/0b/71/4a974fef1ea5bb6de06d4250aebd9f66.ts?key=0cd6168ccb3cfa141adca9ae44747b96b&dis_k=c50e5a36929f57f42e98910912e32360&dis_t=1682995059&dis_dz=CNC-GuangDong_GuangZhou&dis_st=152&src=iqiyi.com&dis_hit=0&dis_tag=01010000&uuid=da6b0c95-64507773-2ce&br=100&mss=1&sd=0&qd_tm=1682995059150&dis_hbr=4&pv=0.1&qd_sc=524db7973f6ac029595c62619f003c19&qd_tvid=3331093954664600&qd_uid=1764530833&qd_p=da6b0c95&qd_vip=1&fr=25&bid=600&qd_index=vod&qd_src=01032001010000000000&dis_src=vrs&qd_did=7fbd512c580d8d5344e6eeef9a1c619e&qd_vipres=0&qd_k=82dce070da85d9d6ee5ca041f43ba2db&cphc=arta").openConnection();
        conn.setRequestProperty("Range", "bytes=54525948-");
        conn.setRequestProperty("User-Agent", "HCDNClient_IOS;libcurl/7.85.0.4 OpenSSL/1.1.1h zlib/1.2.11;QK/10.20.8.7508;NetType/wifi;MSS;ar/110010;QTP/2.20.85.2");
        conn.setRequestProperty("qypid", "3331093954664600_01032001010000000000_600");
        conn.connect();
        InputStream is = conn.getInputStream();
        BufferedOutputStream bos = new BufferedOutputStream(Files.newOutputStream(Paths.get(DOWNLOAD_URL + "/测试.mp4")));
        try {
            byte[] buffer = new byte[1024];
            int len = is.read(buffer, 0, buffer.length);
            while (len > 0) {
                bos.write(buffer, 0, len);
                len = is.read(buffer, 0, buffer.length);
            }
        } finally {
            conn.disconnect();
            bos.close();
        }
    }
}
