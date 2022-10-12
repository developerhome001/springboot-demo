package com.keray.common.qps;

import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;

public class RedisRateLimiterStore implements RateLimiterStore {


    private final RedisTemplate<String, String> redisTemplate;

    public RedisRateLimiterStore(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }


    @Override
    public String getStoreData(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    @Override
    public void setStoreData(String key, String data) {
        redisTemplate.opsForValue().set(key, data, Duration.ofDays(1));
    }
}
