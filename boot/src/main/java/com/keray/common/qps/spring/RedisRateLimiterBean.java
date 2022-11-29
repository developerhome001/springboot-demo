package com.keray.common.qps.spring;

import com.keray.common.exception.QPSFailException;
import com.keray.common.lock.RedissonLock;
import com.keray.common.qps.RateLimiter;
import com.keray.common.qps.RateLimiterStore;
import com.keray.common.qps.RedisRateLimiterStore;
import com.keray.common.qps.RejectStrategy;
import org.redisson.api.RLock;
import org.springframework.data.redis.core.RedisTemplate;

public class RedisRateLimiterBean implements RateLimiterBean<RLock> {

    private final RedissonLock redissonLock;

    private final RateLimiterStore rateLimiterStore;


    public RedisRateLimiterBean(RedissonLock redissonLock, RedisTemplate<String, String> redisTemplate) {
        this.redissonLock = redissonLock;
        this.rateLimiterStore = new RedisRateLimiterStore(redisTemplate);
    }

    public void acquire(String key, String namespace, int maxRate, int acquireCount, int millisecond, int recoveryCount, RejectStrategy rejectStrategy, int waitTime) throws QPSFailException, InterruptedException {
        RateLimiter.acquire(key, namespace, rateLimiterStore, maxRate, redissonLock, acquireCount, millisecond, recoveryCount, rejectStrategy, waitTime);
    }

}