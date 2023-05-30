package com.keray.common.gateway.limit;

import com.keray.common.lock.RedissonLock;
import com.keray.common.qps.spring.MemoryRateLimiterBean;
import com.keray.common.qps.spring.RateLimiterBean;
import com.keray.common.qps.spring.RedisRateLimiterBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(RedisTemplate.class)
public class RateLimiterBeanAutoConfig {
    @Bean
    @ConditionalOnBean(name = "redisTemplate")
    public RateLimiterBean<?> redisRateLimiterBean(RedissonLock redissonLock, @Qualifier("redisTemplate") RedisTemplate<String, String> redisTemplate) {
        return new RedisRateLimiterBean(redissonLock, redisTemplate);
    }

    @Bean
    public RateLimiterBean<?> memoryRateLimiterBean() {
        return new MemoryRateLimiterBean();
    }

}
