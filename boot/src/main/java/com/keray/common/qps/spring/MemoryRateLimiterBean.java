package com.keray.common.qps.spring;

import com.keray.common.exception.QPSFailException;
import com.keray.common.lock.DistributedLock;
import com.keray.common.lock.SingleServerLock;
import com.keray.common.qps.MemoryRateLimiterStore;
import com.keray.common.qps.RateLimiter;
import com.keray.common.qps.RateLimiterStore;
import com.keray.common.qps.RejectStrategy;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MemoryRateLimiterBean implements RateLimiterBean<String> {


    private final RateLimiterStore rateLimiterStore = new MemoryRateLimiterStore();

    private final DistributedLock<String> lock = SingleServerLock.get();


    public void acquire(String key, String namespace, int maxRate, int acquireCount, int millisecond, String appointCron, int recoveryCount, RejectStrategy rejectStrategy, int waitTime) throws QPSFailException, InterruptedException {
        RateLimiter.acquire(key, namespace, rateLimiterStore, maxRate, lock, acquireCount, millisecond, appointCron, recoveryCount, rejectStrategy, waitTime);
    }

}
