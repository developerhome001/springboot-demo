package com.keray.common.config;

import com.keray.common.IUserContext;
import com.keray.common.RateLimiterInterceptor;
import com.keray.common.annotation.RateLimiterApi;
import com.keray.common.exception.QPSFailException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Configuration
@ConditionalOnBean(RateLimiterInterceptor.class)
@Slf4j
public class RateLimiterHandlerInterceptor implements HandlerInterceptor {

    private final RateLimiterInterceptor rateLimiterInterceptor;

    private final IUserContext<?> userContext;

    public RateLimiterHandlerInterceptor(RateLimiterInterceptor rateLimiterInterceptor, IUserContext<?> userContext) {
        this.rateLimiterInterceptor = rateLimiterInterceptor;
        this.userContext = userContext;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (handler instanceof HandlerMethod handlerMethod) {
            var data = handlerMethod.getMethodAnnotation(RateLimiterApi.class);
            if (data == null) {
                return true;
            }
            try {
                rateLimiterInterceptor.interceptor(data, request, response, handler);
            } catch (QPSFailException failException) {
                log.warn("qps异常 ip={},duid={},userId={},agent={}", userContext.currentUserId(), userContext.getDuid(), userContext.currentUserId(), request.getHeader("User-Agent"));
                throw failException;
            }
        }
        return HandlerInterceptor.super.preHandle(request, response, handler);
    }

}
