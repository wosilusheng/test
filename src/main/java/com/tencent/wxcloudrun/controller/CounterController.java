package com.tencent.wxcloudrun.controller;

import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.tencent.wxcloudrun.config.ApiResponse;
import com.tencent.wxcloudrun.dto.CounterRequest;
import com.tencent.wxcloudrun.model.Counter;
import com.tencent.wxcloudrun.service.CounterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

/**
 * counter控制器
 */
@RestController

public class CounterController {

    final CounterService counterService;
    final Logger logger;
    OkHttpClient notFollowRedirectsClient;

    public CounterController(@Autowired CounterService counterService) {
        this.counterService = counterService;
        this.logger = LoggerFactory.getLogger(CounterController.class);
        this.notFollowRedirectsClient = new OkHttpClient().newBuilder()
                .followRedirects(false)
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

    @GetMapping(value = "/api/redirection")
    ApiResponse redirection(@RequestParam(required = false, defaultValue = "") String url) {
        logger.info("/api/redirection get url:" + url);
        if (url == null || url.isEmpty()) {
            return ApiResponse.error("参数有问题");
        }
        Response response = null;
        try {
            Request request = new Request.Builder()
                    .url(url)
                    .build();
            response = notFollowRedirectsClient.newCall(request).execute();
            int code = response.code();
            Headers headers = response.headers();
            if (code == 302 && headers != null) {
                String location = headers.get("Location");
                return ApiResponse.ok(location);
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