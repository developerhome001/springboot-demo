package com.keray.common.gateway.limit;

import com.keray.common.IUserContext;
import com.keray.common.annotation.RateLimiterApi;
import com.keray.common.annotation.RateLimiterGroup;
import com.keray.common.exception.QPSFailException;
import com.keray.common.handler.ServletInvocableHandlerMethodCallback;
import com.keray.common.handler.ServletInvocableHandlerMethodHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.HandlerMethod;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * QPS控制
 */
@Configuration
@ConditionalOnBean(RateLimiterInterceptor.class)
@Slf4j
public class RateLimiterServletInvocableHandlerMethodHandler implements ServletInvocableHandlerMethodHandler {

    private final RateLimiterInterceptor rateLimiterInterceptor;

    private final IUserContext<?> userContext;

    public RateLimiterServletInvocableHandlerMethodHandler(RateLimiterInterceptor rateLimiterInterceptor, IUserContext<?> userContext) {
        log.info("qps流控开启");
        this.rateLimiterInterceptor = rateLimiterInterceptor;
        this.userContext = userContext;
    }

    /**
     * 顺序应该在apilog，和Exception之后
     *
     * @return
     */
    @Override
    public int getOrder() {
        return 300;
    }

    @Override
    public Object work(HandlerMethod handlerMethod, Object[] args, NativeWebRequest request, Map<Object, Object> workContext, ServletInvocableHandlerMethodCallback callback) throws Exception {
        var req = request.getNativeRequest(HttpServletRequest.class);
        if (req == null) return callback.get();
        var group = handlerMethod.getMethodAnnotation(RateLimiterGroup.class);
        if (group != null) {
            for (var da : group.value()) exec(da, req, handlerMethod);
        }
        var data = handlerMethod.getMethodAnnotation(RateLimiterApi.class);
        exec(data, req, handlerMethod);
        try {
            return callback.get();
        } finally {
            // 释放令牌
            if (data != null && data.needRelease()) {
                rateLimiterInterceptor.release(data, req, handlerMethod);
            }
        }
    }


    private void exec(RateLimiterApi data, HttpServletRequest request, HandlerMethod handler) throws Exception {
        if (data == null) {
            return;
        }
        try {
            rateLimiterInterceptor.interceptor(data, request, handler);
        } catch (QPSFailException failException) {
            log.warn("qps异常 ip={},duid={},userId={},agent={}", userContext.currentIp(), userContext.getDuid(), userContext.currentUserId(), request.getHeader("User-Agent"));
            throw failException;
        }
    }


}
