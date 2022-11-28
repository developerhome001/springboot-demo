package com.demo.qps;

import com.keray.common.gateway.limit.DefaultRateLimiterInterceptor;
import com.keray.common.lock.RedissonLock;
import com.keray.common.qps.spring.RateLimiterBean;
import com.keray.common.qps.spring.RedisRateLimiterBean;
import org.redisson.api.RLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

@Configuration
public class Config {

    @Bean
    public RateLimiterBean<RLock> rateLimiterBean(RedissonLock redissonLock, RedisTemplate<String, String> redisTemplate) {
        return new RedisRateLimiterBean(redissonLock, redisTemplate);
    }

    @Bean
    public DefaultRateLimiterInterceptor rateLimiterInterceptor(RateLimiterBean<?> rateLimiterBean) {
        return new DefaultRateLimiterInterceptor(rateLimiterBean);
    }

}
