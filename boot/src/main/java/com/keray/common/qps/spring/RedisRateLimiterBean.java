package com.keray.common.qps.spring;

import com.keray.common.exception.QPSFailException;
import com.keray.common.lock.RedissonLock;
import com.keray.common.qps.*;
import org.redisson.api.RLock;
import org.springframework.data.redis.core.RedisTemplate;

public class RedisRateLimiterBean implements RateLimiterBean<RLock> {

    private final RedissonLock redissonLock;

    private final RateLimiterStore rateLimiterStore;

    public RedisRateLimiterBean(RedissonLock redissonLock, RedisTemplate redisTemplate) {
        this.redissonLock = redissonLock;
        this.rateLimiterStore = new RedisRateLimiterStore(redisTemplate);
    }

    @Override
    public void acquire(RateLimiterParams params) throws QPSFailException, InterruptedException {
        RateLimiter.acquire(params, rateLimiterStore, redissonLock);
    }

    @Override
    public void release(RateLimiterParams params) throws InterruptedException {
        RateLimiter.release(params, rateLimiterStore, redissonLock);
    }
}
