package com.ambitious;

import com.ambitious.v2.util.HttpUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Ambitious
 * @date 2023/10/27
 */
public class HttpUtilsTest {

    public static void main(String[] args) throws Exception {
        testGetRequestRange();
    }

    public static void testGetRequestRange() throws Exception {
        String url = "https://xy106x45x15x161xy.mcdn.bilivideo.cn:8082/v1/resource/1308303232-1-100110.m4s?agrr=1&build=0&buvid=7D2ECCE2-593E-04E8-266C-460947C3E61C38958infoc&bvc=vod&bw=14923&deadline=1698405539&e=ig8euxZM2rNcNbdlhoNvNC8BqJIzNbfqXBvEqxTEto8BTrNvN0GvT90W5JZMkX_YN0MvXg8gNEV4NC8xNEV4N03eN0B5tZlqNxTEto8BTrNvNeZVuJ10Kj_g2UB02J0mN0B5tZlqNCNEto8BTrNvNC7MTX502C8f2jmMQJ6mqF2fka1mqx6gqj0eN0B599M%3D&f=u_0_0&gen=playurlv2&logo=A0000002&mcdnid=2003325&mid=0&nbs=1&nettype=0&oi=1948181741&orderid=0%2C3&os=mcdn&platform=pc&sign=d5f9dd&traceid=trCSnnicrMJpdz_0_e_N&uipk=5&uparams=e%2Cuipk%2Cnbs%2Cdeadline%2Cgen%2Cos%2Coi%2Ctrid%2Cmid%2Cplatform&upsig=4760c32317a7ff60168dfd44aa8119a8";
        String method = "GET";
        Map<String, String> headers = HttpUtils.genDefaultHeaderMapByUrl(null, url);
        // headers.put("Range", "bytes=1-999");
        // headers.put("Range", "bytes=-999");
        // headers.put("Range", "bytes=1-");
        headers.put("Range", "bytes=-");
        long[] ranges = HttpUtils.getRequestRanges(url, method, headers);
        System.out.println(Arrays.toString(ranges));
    }
}
