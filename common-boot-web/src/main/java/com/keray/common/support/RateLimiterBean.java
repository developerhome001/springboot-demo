package com.keray.common.support;

import com.keray.common.exception.QPSFailException;
import com.keray.common.qps.RateLimiter;
import com.keray.common.qps.RateLimiterStore;
import com.keray.common.qps.RedisRateLimiterStore;
import com.keray.common.qps.RejectStrategy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

import javax.annotation.Resource;

@Configuration
@ConditionalOnBean(RedisTemplate.class)
public class RateLimiterBean {

    @Resource
    private RedissonLock redissonLock;

    private final RateLimiterStore rateLimiterStore;


    public RateLimiterBean(RedisTemplate<String, String> redisTemplate) {
        this.rateLimiterStore = new RedisRateLimiterStore(redisTemplate);
    }

    public void acquire(String key, String namespace, int maxRate, int acquireCount, int millisecond, int recoveryCount, RejectStrategy rejectStrategy) throws QPSFailException, InterruptedException {
        RateLimiter.acquire(key, namespace, rateLimiterStore, maxRate, redissonLock, acquireCount, millisecond, recoveryCount, rejectStrategy);
    }

    public void acquire(String key, String namespace, int maxRate, int acquireCount, int millisecond, int recoveryCount, RejectStrategy rejectStrategy, int waitTime) throws QPSFailException, InterruptedException {
        RateLimiter.acquire(key, namespace, rateLimiterStore, maxRate, redissonLock, acquireCount, millisecond, recoveryCount, rejectStrategy, waitTime);
    }

    public void acquire(String key, String namespace, int maxRate, RejectStrategy rejectStrategy) throws QPSFailException, InterruptedException {
        RateLimiter.acquire(key, namespace, rateLimiterStore, maxRate, redissonLock, rejectStrategy);
    }

    public void acquire(String key, String namespace, int maxRate, int acquireCount, RejectStrategy rejectStrategy) throws QPSFailException, InterruptedException {
        RateLimiter.acquire(key, namespace, rateLimiterStore, maxRate, redissonLock, acquireCount, rejectStrategy);
    }

    public void acquire(String key, String namespace, int maxRate, int acquireCount, int recoveryCount, RejectStrategy rejectStrategy) throws QPSFailException, InterruptedException {
        RateLimiter.acquire(key, namespace, rateLimiterStore, maxRate, redissonLock, acquireCount, recoveryCount, rejectStrategy);
    }
}
