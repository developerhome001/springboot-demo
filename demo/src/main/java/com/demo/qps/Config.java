package com.demo.qps;

import com.keray.common.config.KerayRedisConfig;
import com.keray.common.gateway.limit.QpsConfig;
import com.keray.common.lock.RedissonLock;
import com.keray.common.qps.spring.MemoryRateLimiterBean;
import com.keray.common.qps.spring.RateLimiterBean;
import com.keray.common.qps.spring.RedisRateLimiterBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

@Configuration
@AutoConfigureAfter(KerayRedisConfig.class)
public class Config {

    @Bean
    public RateLimiterBean<?> redisRateLimiterBean(RedissonLock redissonLock, @Qualifier("redisTemplate") RedisTemplate<String, String> redisTemplate) {
        return new RedisRateLimiterBean(redissonLock, redisTemplate);
    }

    @Bean
    public RateLimiterBean<?> memoryRateLimiterBean() {
        return new MemoryRateLimiterBean();
    }

    @Bean
    public DefaultRateLimiterInterceptor rateLimiterInterceptor(QpsConfig qpsConfig) {
        return new DefaultRateLimiterInterceptor(qpsConfig);
    }


}
