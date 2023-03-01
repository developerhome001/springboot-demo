package com.keray.common;

import com.keray.common.exception.QPSFailException;
import com.keray.common.qps.RateLimiterParams;
import com.keray.common.qps.RejectStrategy;
import com.keray.common.qps.spring.MemoryRateLimiterBean;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;

public class MemoryRateTest {

    private MemoryRateLimiterBean rateLimiterBean = new MemoryRateLimiterBean();

    @Test
    public void storeTest() throws InterruptedException, QPSFailException {
        var cnt = 10;
        var latch = new CountDownLatch(cnt);
        System.out.println("xxxxxxxxxx");
        for (var i = 0; i < cnt; i++) {
            rateLimiterBean.acquire(new RateLimiterParams()
                    .setMaxRate(1)
                    .setMillisecond(10)
                    .setRejectStrategy(RejectStrategy.wait)
                    .setWaitTime(200)
                    .setWaitSpeed(1)
            );
            int finalI = i;
            System.out.println(System.currentTimeMillis() + "  " + finalI);
        }
        latch.await();
    }

}
