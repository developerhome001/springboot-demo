package com.keray.common.qps.spring;

import com.keray.common.exception.QPSFailException;
import com.keray.common.lock.DistributedLock;
import com.keray.common.lock.SingleServerLock;
import com.keray.common.qps.MemoryRateLimiterStore;
import com.keray.common.qps.RateLimiter;
import com.keray.common.qps.RateLimiterParams;
import com.keray.common.qps.RateLimiterStore;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MemoryRateLimiterBean implements RateLimiterBean<String> {


    private final RateLimiterStore rateLimiterStore = new MemoryRateLimiterStore();

    private final DistributedLock<String> lock = SingleServerLock.get();


    @Override
    public void acquire(RateLimiterParams params) throws QPSFailException, InterruptedException {
        RateLimiter.acquire(params, rateLimiterStore, lock);
    }

    @Override
    public void release(RateLimiterParams params) throws InterruptedException {
        RateLimiter.release(params, rateLimiterStore, lock);
    }
}
