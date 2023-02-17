package com.demo;

import com.keray.common.annotation.ApiResult;
import com.keray.common.annotation.RateLimiterApi;
import com.keray.common.annotation.RateLimiterGroup;
import com.keray.common.cache.CacheConstants;
import com.keray.common.gateway.limit.RateLimiterApiTarget;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/")
public class Api {

    @Resource
    private RedisTemplate redisTemplate;


    @Resource
    private RedisTemplate cacheRedisRedisTemplate;


    @Resource
    private RedisTemplate persistenceRedisTemplate;


    @ApiResult
    @GetMapping("/test")
//    @Cacheable(value = CacheConstants.SMALL_UP_UP)
    @RateLimiterGroup(value = {
            @RateLimiterApi(namespace = "test", maxRate = 2, appointCron = "0 0 * * * *", recoveryCount = 2),
            @RateLimiterApi(namespace = "test", maxRate = 5, appointCron = "0 0 * * * *", recoveryCount = 5, target = RateLimiterApiTarget.ip)
    })
    public Object checkHealth(@RequestParam(defaultValue = "aaa") String a) {
        var x = System.currentTimeMillis();
        redisTemplate.opsForValue().set("123", x, 10, TimeUnit.HOURS);
        cacheRedisRedisTemplate.opsForValue().set("123", x, 10, TimeUnit.HOURS);
        persistenceRedisTemplate.opsForValue().set("123", x, 10, TimeUnit.HOURS);
        System.out.println("xxxxxxxxx");
        return x;
    }

}
