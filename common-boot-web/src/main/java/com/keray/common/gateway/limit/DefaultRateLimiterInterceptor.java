package com.keray.common.gateway.limit;

import cn.hutool.core.util.StrUtil;
import com.keray.common.annotation.RateLimiterApi;
import com.keray.common.exception.QPSFailException;
import com.keray.common.qps.spring.RateLimiterBean;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class DefaultRateLimiterInterceptor extends AbstractRateLimiterInterceptor {

    private final RateLimiterBean<?> rateLimiter;

    public DefaultRateLimiterInterceptor(RateLimiterBean<?> rateLimiter) {
        this.rateLimiter = rateLimiter;
    }


    @Override
    public void interceptor(RateLimiterApi data, HttpServletRequest request, HttpServletResponse response, Object handler) throws InterruptedException, QPSFailException {
        var key = annDataGetKey(data);
        if (StrUtil.isNotEmpty(key)) {
            rateLimiter.acquire(key, data.namespace(), data.maxRate(), 1, data.millisecond(), data.recoveryCount(), data.rejectStrategy());
        }
    }
}
