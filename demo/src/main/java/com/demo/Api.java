package com.demo;

import com.keray.common.annotation.ApiResult;
import com.keray.common.cache.CacheConstants;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class Api {


    @ApiResult
    @GetMapping("/test")
    @Cacheable(value = CacheConstants.SMALL_UP_UP)
    public Object checkHealth(@RequestParam(defaultValue = "aaa") String a) {
        System.out.println("xxxxxxxxx");
        return System.currentTimeMillis();
    }

}
