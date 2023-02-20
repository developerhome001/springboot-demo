package com.keray.common.gateway.limit;

import cn.hutool.core.util.StrUtil;
import com.keray.common.annotation.RateLimiterApi;
import com.keray.common.exception.QPSFailException;
import com.keray.common.qps.spring.RateLimiterBean;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.HandlerMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

public class DefaultRateLimiterInterceptor extends AbstractRateLimiterInterceptor {

    private final RateLimiterBean<?> rateLimiter;

    public DefaultRateLimiterInterceptor(RateLimiterBean<?> rateLimiter) {
        this.rateLimiter = rateLimiter;
    }


    @Override
    public boolean interceptorConsumer(NativeWebRequest request, HandlerMethod handler, Map<String, QpsData> releaseList) throws InterruptedException, QPSFailException {
        return false;
    }

    @Override
    public void interceptor(RateLimiterApi data, NativeWebRequest request, HandlerMethod handler, Map<String, QpsData> releaseList) throws InterruptedException, QPSFailException {
        var uuid = annDataGetKey(data);
        if (StrUtil.isNotEmpty(uuid)) {
            try {
                rateLimiter.acquire(uuid, data.namespace(), data.maxRate(), 1, data.millisecond(), data.appointCron(), data.recoveryCount(),
                        data.rejectStrategy(), data.waitTime(), data.waitSpeed(), data.needRelease());
                if (data.needRelease()) releaseList.put(uuid, QpsData.of(data));
            } catch (QPSFailException e) {
                if (StrUtil.isNotBlank(data.rejectMessage()))
                    throw new QPSFailException(data.limitType() == RateLimitType.system, data.rejectMessage());
                throw new QPSFailException(data.limitType() == RateLimitType.system);
            }
        }
    }

    @Override
    public void release(String key, QpsData qpsData, NativeWebRequest request, HandlerMethod handler) throws InterruptedException {
        if (StrUtil.isNotEmpty(key)) {
            rateLimiter.release(key, qpsData.getNamespace(), qpsData.getMaxRate(), qpsData.getReleaseCnt());
        }
    }
}
