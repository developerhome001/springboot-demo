package com.keray.common.gateway.limit;

import com.keray.common.IUserContext;
import com.keray.common.annotation.RateLimiterApi;

import javax.annotation.Resource;

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
