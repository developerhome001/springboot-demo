package com.keray.common.gateway.limit;

import com.keray.common.annotation.RateLimiterApi;
import com.keray.common.exception.QPSFailException;
import org.springframework.web.method.HandlerMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface RateLimiterInterceptor {
    /**
     * QPS拦截
     *
     * @param data
     * @param request
     * @param handler
     * @throws InterruptedException
     * @throws QPSFailException
     */
    void interceptor(RateLimiterApi data, HttpServletRequest request, HandlerMethod handler) throws InterruptedException, QPSFailException;

    /**
     * QPS执行完成后释放
     *
     * @param data
     * @param request
     * @param handler
     * @throws InterruptedException
     */
    void release(RateLimiterApi data, HttpServletRequest request, HandlerMethod handler) throws InterruptedException;
}
