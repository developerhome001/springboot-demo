package com.demo;

import com.demo.mapper.TestModelMapper;
import com.demo.model.TestModel;
import com.keray.common.Wrappers;
import com.keray.common.annotation.ApiResult;
import com.keray.common.annotation.RateLimiterApi;
import com.keray.common.cache.CacheConstants;
import com.keray.common.cache.CacheTime;
import com.keray.common.diamond.Diamond;
import com.keray.common.diamond.handler.DiamondHandler;
import com.keray.common.exception.BizRuntimeException;
import com.keray.common.gateway.downgrade.ApiDowngrade;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
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

    @Resource
    private TestModelMapper testModelMapper;

    @Diamond("test")
    private String test = "test";


    @ApiResult
    @GetMapping("/test")
    @Cacheable(value = CacheConstants.M30)
    @CacheTime(100000)
//    @RateLimiterGroup(value = {
//            @RateLimiterApi(namespace = "test", maxRate = 2, appointCron = "0 0 * * * *", recoveryCount = 2),
//            @RateLimiterApi(namespace = "test", maxRate = 5, appointCron = "0 0 * * * *", recoveryCount = 5, target = RateLimiterApiTarget.ip)
//    })
    @RateLimiterApi(namespace = "test", maxRate = 1, needRelease = true)
//    @ApiDowngrade(json = "1234", timeout = 500)
    public Object checkHealth(@RequestParam(defaultValue = "aaa") String a) throws Exception {
//        var sleep = RandomUtil.randomInt(100, 1000);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new BizRuntimeException("线程中断了");
        }
//        throw new BizRuntimeException("直接抛出异常");
        return 123;
    }

    @Resource
    private DiamondHandler diamondHandler;

    @GetMapping("/test1")
    public void test1(String value) {
        diamondHandler.handler("test", value);
    }

    @GetMapping("/test11")
    public String test11() {
        return test;
    }

    @GetMapping("/test2")
    public void test2(String id) {
        var m = new TestModel();
        m.setId(id);
        m.setName("test-2");
        m.setCode("test-2");
        testModelMapper.updateById(m);
    }

    @GetMapping("/test3")
    public void test3() {
        testModelMapper.update(null, Wrappers.<TestModel>lambdaUpdate()
                .set(TestModel::getName, "test-3")
                .set(TestModel::getCode, "test-2")
                .eq(TestModel::getCode, "test-2")
        );
    }

    public void setTest(String test) {
        this.test = test;
    }
}
