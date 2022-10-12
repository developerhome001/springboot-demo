package com.keray.common;

import com.keray.common.annotation.RateLimiterApi;
import com.keray.common.qps.RateLimiter;
import com.keray.common.qps.RateLimiterStore;
import com.keray.common.qps.RedisRateLimiterStore;
import com.keray.common.support.RedissonLock;
import org.redisson.api.RLock;
import org.springframework.data.redis.core.RedisTemplate;

import javax.annotation.Resource;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractRateLimiterInterceptor implements RateLimiterInterceptor {

    @Resource
    protected IUserContext<?> userContext;


    protected String annDataGetKey(RateLimiterApi data) {
        if (data.target() == RateLimiterApiTarget.namespace) {
            return data.namespace();
        } else if (data.target() == RateLimiterApiTarget.ip) {
            return userContext.currentIp();
        } else if (data.target() == RateLimiterApiTarget.duid) {
            return userContext.getDuid();
        } else if (data.target() == RateLimiterApiTarget.userOrDuid) {
            if (userContext.loginStatus()) return userContext.currentUserId();
            return userContext.getDuid();
        } else {
            return userContext.currentUserId();
        }
    }
}
