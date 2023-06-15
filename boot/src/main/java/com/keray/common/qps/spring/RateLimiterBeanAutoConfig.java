package com.keray.common.qps.spring;

import com.keray.common.lock.RedissonLock;
import org.redisson.spring.starter.RedissonAutoConfiguration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

@Configuration(proxyBeanMethods = false)
public class RateLimiterBeanAutoConfig {

    @ConditionalOnClass(RedissonAutoConfiguration.class)
    @Bean
    public RateLimiterBean<?> redisRateLimiterBean(RedissonLock redissonLock, @Qualifier("redisTemplate") RedisTemplate<String, String> redisTemplate) {
        return new RedisRateLimiterBean(redissonLock, redisTemplate);
    }

    @Bean
    public RateLimiterBean<?> memoryRateLimiterBean() {
        return new MemoryRateLimiterBean();
    }

}
