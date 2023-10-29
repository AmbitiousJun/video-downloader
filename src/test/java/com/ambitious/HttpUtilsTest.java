package com.ambitious;

import com.ambitious.v2.config.Config;
import com.ambitious.v2.util.HttpUtils;
import okhttp3.Headers;
import okhttp3.Request;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Ambitious
 * @date 2023/10/27
 */
public class HttpUtilsTest {

    public static void main(String[] args) throws Exception {
        testDownloadWithRateLimit();
    }

    public static void testDownloadWithRateLimit() throws Exception {
        Config.load();
        String url = "https://pcvideomigu.titan.mgtv.com/c1/2023/10/28_0/198F36FA586D55B87A7F23026F190723_20231028_1_1_2538_mp4/B2F18BD0C5CD1588DC2B3FA29F0DF238_0_5080_3116_v02_mp4.ts?arange=0&pm=~TULvUEZ~0bilw1ycEyBg~0iMChdAJ5vJTMkIOOT7xyCGh7Msst9eeWcLWx4dnPNjpiBes1Zwx~3wIp3TGGHJvsjr73w46NxinteDpsp2K8hBSz1tdpi4Zm~og3YhWyYmU_16~G6tU~klg57WXK1N2GMm__4R9sGk4bK_Nf4EaWvjtka0ZxmnXy0dTO2kyNddqPMwfvZLYShAmoyuFCmDlqZs1ipWQ2O96Vpbv1xWiIJZr_T_rq_9_2cf5SAo6xVArhseG9A~~EaufKnF7WDZ6M5jaWiacIiBASPc9N_o4Lz_s6WsS93NQL_KJZ7QTvD4UZqyzK0_kZ57BQYGQfYmAWh0s_vmT4S8mdWLmkyaDIASGrxdQVcZEAHs8_L9PwjwJw73c9ot1BEVX2woDUKNDG9vaNR3Gc3cJF0jEurVS3ZTZs9QjF34SPcG8B464mM59rubtfIx8s-&mr=CrVpfNLygONAoLlROagXe9A2xELSqH4NFZtMYeJ7tDIcmDw_H0jQ_klwFxO5cLIRn8BYzKN_aMp4tw131iLGbdy~YhKmgXWtaQVyIxSmNCwoCLGXe10mbckhSLM7U0LEX1b5udC_60L6zVZ6cTc6HeqHgBXiNYft46UsZBWAajtXwaSIQ21EYIOl2qrgmHxjeqgiZgm3GTmDE345wh8yEBTfKrtx6XD0vEMbTsCUQ~0u6SYxS2N4MsXYMTNe_4Kqy1ImyQLAEVsfSjV~OJW4bYaU0lZnzZEYCz3xKM0sD8mVap8CWab5ZCkvr4Bm2MKP70uXTjw4tXsmNFSkmFmxRxXNKmctxP7hUmIKY~K~2nne6NwCz5TUcchGgpHhkRw0kYDIZIpfu8rjca66puTLAzkE1o06Bf6A4ywd_KhuBUtMS8pdb9KwdizvISAUJHMHJKs7am30Tpk~p2YREDB_R6JYSXwJb9NZi9ehJjH4Zfye42cEJetaEQMjyN1PsyDLc4USLcVkAyDh~FZktthJ4qPnNXe5~0lsMf94Km~Mf1625CoJKnLtEw31TowICkuZu0eOSZM1gjvlEIwi1Tw3xHBWW6aA~VPWEKzXNGMfG~nbWDU88CfkyVajxtfCth_yQwe3SGWCfpOt24UjC2ZLEA--&uid=e4f3fabc8ec345b49c021c67e1c2a082&scid=25121&cpno=6i06rp&ruid=4ba4d6941cb24986&sh=1";
        File dest = new File("/Users/ambitious/Downloads/test.ts");
        Request request = new Request.Builder()
                .url(url)
                .headers(Headers.of(HttpUtils.genDefaultHeaderMapByUrl(null, url)))
                .get()
                .build();
        HttpUtils.downloadWithRateLimit(request, dest);
        System.exit(0);
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
