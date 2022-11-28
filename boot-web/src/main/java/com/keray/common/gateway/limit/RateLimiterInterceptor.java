package com.keray.common.gateway.limit;

import com.keray.common.annotation.RateLimiterApi;
import com.keray.common.exception.QPSFailException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface RateLimiterInterceptor {
    void interceptor(RateLimiterApi data, HttpServletRequest request, HttpServletResponse response, Object handler) throws InterruptedException, QPSFailException;
}
