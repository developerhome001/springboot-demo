package com.keray.common.support;

import cn.hutool.core.util.StrUtil;
import com.keray.common.AbstractRateLimiterInterceptor;
import com.keray.common.RateLimiterInterceptor;
import com.keray.common.annotation.RateLimiterApi;
import com.keray.common.exception.QPSFailException;
import com.keray.common.qps.RateLimiter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@ConditionalOnMissingBean(RateLimiterInterceptor.class)
@ConditionalOnBean(RateLimiterBean.class)
@Configuration
public class DefaultRateLimiterInterceptor extends AbstractRateLimiterInterceptor {

    private final RateLimiterBean rateLimiter;

    public DefaultRateLimiterInterceptor(RateLimiterBean rateLimiter) {
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
