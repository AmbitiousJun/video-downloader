package com.ambitious;

import com.ambitious.v2.util.HttpUtils;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.IOException;

/**
 * okhttp 相关测试
 * @author Ambitious
 * @date 2023/10/27
 */
public class OkHttpTest {

    public static void main(String[] args) throws Exception {

    }

    public static void testRedirect() throws IOException {
        // 测试 okhttp 的自动重定向
        OkHttpClient client = HttpUtils.getOkHttpClient();
        Request request = new Request.Builder()
                .url("http://x.x.x.x:9999/iptv?type=cctv13")
                .get()
                .build();
        try (Response res = client.newCall(request).execute()) {
            int code = res.code();
            ResponseBody body = res.body();
            if (body != null) {
                String bodyString = body.string();
            }
            System.out.println();
        }
    }
}
