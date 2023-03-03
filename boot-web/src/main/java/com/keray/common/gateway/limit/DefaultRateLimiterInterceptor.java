package com.keray.common.gateway.limit;

import com.keray.common.qps.spring.RateLimiterBean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import javax.annotation.Resource;

@Slf4j
@Configuration
@ConditionalOnMissingBean(RateLimiterInterceptor.class)
public class DefaultRateLimiterInterceptor extends AbstractRateLimiterInterceptor {

    @Resource
    protected RateLimiterBean redisRateLimiterBean;

    @Resource
    private RateLimiterBean memoryRateLimiterBean;

    @Resource
    @Lazy
    private QpsConfig qpsConfig;

    public DefaultRateLimiterInterceptor() {
        log.info("注入默认QPS处理器");
    }

    @Override
    public QpsConfig getQpsConfig() {
        return qpsConfig;
    }

    @Override
    protected RateLimiterBean getBean(String name) {
        if ("redisRateLimiterBean".equals(name)) return redisRateLimiterBean;
        if ("redis".equals(name)) return redisRateLimiterBean;
        return memoryRateLimiterBean;
    }
}
