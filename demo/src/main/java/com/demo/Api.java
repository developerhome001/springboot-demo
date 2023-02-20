package com.demo;

import cn.hutool.core.util.RandomUtil;
import com.keray.common.annotation.ApiResult;
import com.keray.common.annotation.RateLimiterApi;
import com.keray.common.exception.BizRuntimeException;
import com.keray.common.gateway.downgrade.ApiDowngrade;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/")
@Slf4j
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
//    @RateLimiterGroup(value = {
//            @RateLimiterApi(namespace = "test", maxRate = 2, appointCron = "0 0 * * * *", recoveryCount = 2),
//            @RateLimiterApi(namespace = "test", maxRate = 5, appointCron = "0 0 * * * *", recoveryCount = 5, target = RateLimiterApiTarget.ip)
//    })
    @RateLimiterApi(namespace = "test", maxRate = 1, needRelease = true)
    @ApiDowngrade(json = "1234", timeout = 800)
    public Object checkHealth(@RequestParam(defaultValue = "aaa") String a) throws Exception {
        var sleep = RandomUtil.randomInt(100, 1000);
//        try {
        Thread.sleep(20000);
//        } catch (InterruptedException e) {
//            throw new BizRuntimeException("线程中断了");
//        }
//        throw new BizRuntimeException("直接抛出异常");
        return 123;
    }
}
