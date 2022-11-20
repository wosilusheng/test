package com.tencent.wxcloudrun.controller;

import com.tencent.wxcloudrun.config.ApiResponse;
import com.tencent.wxcloudrun.dto.CounterRequest;
import com.tencent.wxcloudrun.model.Counter;
import com.tencent.wxcloudrun.service.CounterService;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestBody;

import javax.servlet.http.HttpServletResponse;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.net.URL;
import java.util.*;

/**
 * counter控制器
 */
@RestController

public class CounterController {

    final CounterService counterService;
    final Logger logger;
//    OkHttpClient notFollowRedirectsClient;
    OkHttpClient client;
    List<String> urls = Arrays.asList("https://v3-dy-o.zjcdn.com;https://v95-sz.douyinvod.com;https://v26.douyinvod.com;https://v11.douyinvod.com;https://v3-z.douyinvod.com;https://v6-x.douyinvod.com;https://v5-g.douyinvod.com;https://v5-i.douyinvod.com;https://v3.douyinvod.com;https://v5-e.douyinvod.com;https://v95.douyinvod.com;https://v5-j.douyinvod.com;https://v9-traffic.douyinvod.com;https://v9.douyinvod.com;https://v3-x.douyinvod.com;https://v6.douyinvod.com;https://v95-sh.douyinvod.com;https://v1.douyinvod.com;https://v11-x.douyinvod.com;https://v3-y.douyinvod.com;https://v5-f.douyinvod.com;https://v95-p.douyinvod.com;https://v27.douyinvod.com;https://v95-hb.douyinvod.com;https://v5-h.douyinvod.com;https://v5.douyinvod.com;https://v27-a.douyinvod.com;https://v83-016.douyinvod.com;https://v95-hn.douyinvod.com;https://v95-zj.douyinvod.com;https://v95-sz-cold.douyinvod.com".split(";"));

    Set<String> needAddUrls = new HashSet<>();

    public CounterController(@Autowired CounterService counterService) {
        this.counterService = counterService;
        this.logger = LoggerFactory.getLogger(CounterController.class);
//        this.notFollowRedirectsClient = new OkHttpClient().newBuilder()
//                .followRedirects(false)
//                .build();
        this.client = new OkHttpClient().newBuilder()
                .build();
    }


    /**
     * 获取当前计数
     *
     * @return API response json
     */
    @GetMapping(value = "/api/count")
    ApiResponse get() {
        logger.info("/api/count get request");
        Optional<Counter> counter = counterService.getCounter(1);
        Integer count = 0;
        if (counter.isPresent()) {
            count = counter.get().getCount();
        }

        return ApiResponse.ok(count);
    }

    @GetMapping(value = "/api/hello")
    ApiResponse getHello() {
        logger.info("/api/hello get request");


        return ApiResponse.ok("hello world");
    }

    @GetMapping(value = "/api/needAddUrls")
    ApiResponse getNeedAddUrls() {
        logger.info("/api/needAddUrls  request");
        return ApiResponse.ok(needAddUrls);
    }

    @GetMapping(value = "/api/redirection")
    ApiResponse redirection(@RequestParam(required = false, defaultValue = "") String url) {
        logger.info("/api/redirection get url:" + url);
        if (url == null || url.isEmpty()) {
            return ApiResponse.error("参数有问题");
        }
        Response response = null;
        try {
            FormBody formBody = new FormBody.Builder()
                    .add("f", "true")
                    .add("w", url)
                    .build();
            Request request = new Request.Builder()
                    .header("referer", "https://redirectdetective.com/")
                    .url("https://redirectdetective.com/ld.px")
                    .post(formBody)
                    .build();
            response = client.newCall(request).execute();
            int code = response.code();
            if (code == 200) {
                String string = response.body().string();
                int index = string.lastIndexOf("https://");
                if (index != -1) {
                    int endIndex = string.indexOf("</a>", index);
                    if (endIndex > index) {
                        String resultUrl = string.substring(index, endIndex).trim();
                        String authority = new URL(resultUrl).getAuthority();
                        String tempUrl = "https://" + authority;
                        if (!urls.contains(tempUrl)) {
                            logger.error("url:" + tempUrl + "没有收藏");
                            needAddUrls.add(tempUrl);
                        }
                        logger.info("/api/redirection result:" + resultUrl);
                        return ApiResponse.ok(resultUrl);
                    }
                }
            }
            response.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (response != null) {
                response.close();
            }
        }
        return ApiResponse.error("解析失败");
    }

//    @GetMapping(value = "/api/redirection2")
//    ApiResponse redirection2(@RequestParam(required = false, defaultValue = "") String url) {
//        logger.info("/api/redirection get url:" + url);
//        if (url == null || url.isEmpty()) {
//            return ApiResponse.error("参数有问题");
//        }
//        Response response = null;
//        try {
//            Request request = new Request.Builder()
//                    .url(url)
//                    .build();
//            response = notFollowRedirectsClient.newCall(request).execute();
//            int code = response.code();
//            Headers headers = response.headers();
//            if (code == 302 && headers != null) {
//                String location = headers.get("Location");
//                String authority = new URL(location).getAuthority();
//                String tempUrl = "https://" + authority;
//                if (!urls.contains(tempUrl)) {
//                    logger.error("url:" + tempUrl + "没有收藏");
//                    needAddUrls.add(tempUrl);
//                }
//                return ApiResponse.ok(location);
//            }
//
//            response.close();
//        } catch (Exception e) {
//            e.printStackTrace();
//        } finally {
//            if (response != null) {
//                response.close();
//            }
//        }
//        return ApiResponse.error("解析失败");
//    }


    /**
     * 更新计数，自增或者清零
     *
     * @param request {@link CounterRequest}
     * @return API response json
     */
    @PostMapping(value = "/api/count")
    ApiResponse create(@RequestBody CounterRequest request) {
        logger.info("/api/count post request, action: {}", request.getAction());

        Optional<Counter> curCounter = counterService.getCounter(1);
        if (request.getAction().equals("inc")) {
            Integer count = 1;
            if (curCounter.isPresent()) {
                count += curCounter.get().getCount();
            }
            Counter counter = new Counter();
            counter.setId(1);
            counter.setCount(count);
            counterService.upsertCount(counter);
            return ApiResponse.ok(count);
        } else if (request.getAction().equals("clear")) {
            if (!curCounter.isPresent()) {
                return ApiResponse.ok(0);
            }
            counterService.clearCount(1);
            return ApiResponse.ok(0);
        } else {
            return ApiResponse.error("参数action错误");
        }
    }

}