package com.keray.common.gateway.limit;

import cn.hutool.core.util.StrUtil;
import com.keray.common.IUserContext;
import com.keray.common.annotation.QpsIgnore;
import com.keray.common.annotation.RateLimiterApi;
import com.keray.common.annotation.RateLimiterGroup;
import com.keray.common.exception.QPSFailException;
import com.keray.common.gateway.downgrade.ApiDowngradeServletInvocableHandlerPipeline;
import com.keray.common.gateway.downgrade.Node;
import com.keray.common.handler.ServletInvocableHandlerMethodCallback;
import com.keray.common.handler.ServletInvocableHandlerPipeline;
import com.keray.common.qps.RateLimiterParams;
import com.keray.common.qps.RejectStrategy;
import com.keray.common.qps.spring.RateLimiterBean;
import com.keray.common.service.AiService;
import com.keray.common.util.HttpWebUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.HandlerMethod;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * QPS控制
 */
@Configuration
@ConditionalOnBean(RateLimiterInterceptor.class)
@Slf4j
public class RateLimiterServletInvocableHandlerPipeline implements ServletInvocableHandlerPipeline {

    private final RateLimiterInterceptor rateLimiterInterceptor;

    private final IUserContext<?> userContext;


    @Resource
    private RateLimiterBean memoryRateLimiterBean;

    public RateLimiterServletInvocableHandlerPipeline(RateLimiterInterceptor rateLimiterInterceptor, IUserContext<?> userContext) {
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
        return 400;
    }

    @Override
    public Object work(HandlerMethod handlerMethod, Object[] args, NativeWebRequest request, Map<Object, Object> workContext, ServletInvocableHandlerMethodCallback callback) throws Exception {
        // 忽略的接口
        if (rateLimiterInterceptor.requestIgnoreRateLimiter(RateLimiterInterceptor.RateLimiterStep.start, null, request, handlerMethod)) {
            return callback.get();
        }
        var releaseData = new LinkedList<QpsData>();
        Runnable run = () -> {
            // 添加接口降级超时处理勾子
            // 如果是需要释放型的令牌，给超时处理管道添加勾子
            // 管道存在超时处理的勾子链条
            var hooks = (List<Runnable>) workContext.get(ApiDowngradeServletInvocableHandlerPipeline.HOOKS_KEY);
            if (hooks != null && !releaseData.isEmpty()) {
                releaseData.forEach(v -> hooks.add(() -> {
                    try {
                        rateLimiterInterceptor.release(v.getKey(), v, request, handlerMethod);
                    } catch (InterruptedException e) {
                        log.error("超时令牌释放失败");
                    }
                }));
            }
        };
        try {
            try {
                var f = rateLimiterInterceptor.requestIgnoreRateLimiter(RateLimiterInterceptor.RateLimiterStep.consumer, null, request, handlerMethod);
                var haveWork = !f && rateLimiterInterceptor.interceptorConsumer(request, handlerMethod, releaseData);
                if (haveWork) {
                    run.run();
                    return callback.get();
                }
                var group = handlerMethod.getMethodAnnotation(RateLimiterGroup.class);
                if (group != null) {
                    for (var da : group.value()) exec(da, request, handlerMethod, releaseData);
                }
                var data = handlerMethod.getMethodAnnotation(RateLimiterApi.class);
                if (data != null) {
                    exec(data, request, handlerMethod, releaseData);
                }
            } catch (QPSFailException e) {
                if (!rateLimiterInterceptor.failCall(request.getNativeRequest(HttpServletRequest.class), request.getNativeResponse(HttpServletResponse.class), handlerMethod, e)) {
                    throw e;
                }
            }
            run.run();
            return callback.get();
        } finally {
            // 释放令牌
            var node = (Node) workContext.get(ApiDowngradeServletInvocableHandlerPipeline.CONTEXT_NODE);
            if (!releaseData.isEmpty() && (node == null || !node.isTimeout())) {
                for (var d : releaseData)
                    rateLimiterInterceptor.release(d.getKey(), d, request, handlerMethod);
            }
        }
    }


    private void exec(RateLimiterApi data, NativeWebRequest request, HandlerMethod handler, List<QpsData> releaseList) throws Exception {
        try {
            if (rateLimiterInterceptor.requestIgnoreRateLimiter(RateLimiterInterceptor.RateLimiterStep.annotation, null, request, handler)) return;
            rateLimiterInterceptor.interceptor(data, request, handler, releaseList);
        } catch (QPSFailException e) {
            log.warn("qps异常 ip={},duid={},userId={},agent={}", userContext.currentIp(), userContext.getDuid(), userContext.currentUserId(), request.getHeader("User-Agent"));
            if (StrUtil.isNotBlank(data.rejectMessage()))
                throw new QPSFailException(data.limitType() == RateLimitType.system, data.rejectMessage(), e.getParams());
            throw new QPSFailException(data.limitType() == RateLimitType.system, e.getParams());
        }
    }

}
