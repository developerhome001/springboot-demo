package com.keray.common.cache;

import org.springframework.cache.support.SimpleValueWrapper;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.lang.Nullable;

import java.time.Duration;
import java.util.Map;

public class KerayRedisCache extends RedisCache {
    private final String name;
    private final RedisCacheWriter cacheWriter;
    private final RedisCacheConfiguration cacheConfig;

    private final Map<String, Long> consumerTtl;

    protected KerayRedisCache(String name, RedisCacheWriter cacheWriter, RedisCacheConfiguration cacheConfig, Map<String, Long> consumerTtl) {
        super(name, cacheWriter, cacheConfig);
        this.name = name;
        this.cacheWriter = cacheWriter;
        this.cacheConfig = cacheConfig;
        this.consumerTtl = consumerTtl;
    }

    @Override
    public void put(Object key, @Nullable Object value) {
        Object cacheValue = preProcessCacheValue(value);
        if (!isAllowNullValues() && cacheValue == null) {
            throw new IllegalArgumentException(String.format(
                    "Cache '%s' does not allow 'null' values; Avoid storing null via '@Cacheable(unless=\"#result == null\")' or configure RedisCache to allow 'null' via RedisCacheConfiguration",
                    name));
        }
        cacheWriter.put(name, createAndConvertCacheKey(key), serializeCacheValue(cacheValue), getTtl());
    }

    @Override
    public ValueWrapper putIfAbsent(Object key, @Nullable Object value) {
        Object cacheValue = preProcessCacheValue(value);
        if (!isAllowNullValues() && cacheValue == null) {
            return get(key);
        }
        byte[] result = cacheWriter.putIfAbsent(name, createAndConvertCacheKey(key), serializeCacheValue(cacheValue), getTtl());
        if (result == null) {
            return null;
        }
        return new SimpleValueWrapper(fromStoreValue(deserializeCacheValue(result)));
    }

    public Duration getTtl() {
        var ttl = KerayCacheAspectSupport.TTL.get();
        if (ttl != null) return Duration.ofMillis(ttl);
        var time = consumerTtl.get(name);
        if (time != null) {
            if (time <= 0) return null;
            return Duration.ofMillis(time);
        }
        return cacheConfig.getTtl();
    }

    private byte[] createAndConvertCacheKey(Object key) {
        return serializeCacheKey(createCacheKey(key));
    }
}
