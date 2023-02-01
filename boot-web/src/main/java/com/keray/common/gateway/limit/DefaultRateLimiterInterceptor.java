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
        var uuid = annDataGetKey(data);
        if (StrUtil.isNotEmpty(uuid)) {
            try {
                rateLimiter.acquire(uuid, data.namespace(), data.maxRate(), 1, data.millisecond(), data.appointCron(), data.recoveryCount(),
                        data.rejectStrategy(), data.waitTime(), data.waitSpeed());
            } catch (QPSFailException e) {
                if (StrUtil.isNotBlank(data.rejectMessage())) throw new QPSFailException(data.rejectMessage());
                throw e;
            }
        }
    }
}
