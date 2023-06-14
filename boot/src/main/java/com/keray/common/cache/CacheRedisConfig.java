package com.keray.common.cache;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.AbstractCachingConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheInterceptor;
import org.springframework.cache.interceptor.CacheOperationSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;

/**
 * @author by keray
 * date:2020/4/18 5:01 下午
 */
@Configuration
@Import(CacheProperties.class)
@EnableCaching
@ConditionalOnClass(RedisTemplate.class)
public class CacheRedisConfig extends AbstractCachingConfiguration {

    @ConditionalOnProperty(value = "spring.cache.type", havingValue = "redis")
    @ConditionalOnClass(RedisTemplate.class)
    @Bean
    @Order(0)
    @Primary
    public RedisCacheManager cacheManager(CacheProperties cacheProperties, @Qualifier("cacheRedisRedisTemplate") RedisTemplate cacheRedisRedisTemplate, CacheConstants cacheConstants) {
        var redisCacheConfiguration = createConfiguration(cacheProperties, cacheRedisRedisTemplate);
        var write = RedisCacheWriter.lockingRedisCacheWriter(cacheRedisRedisTemplate.getConnectionFactory());
        return new RedisCacheManager(write, redisCacheConfiguration) {
            @Override
            protected RedisCache createRedisCache(String name, RedisCacheConfiguration cacheConfig) {
                return new KerayRedisCache(name, write, cacheConfig != null ? cacheConfig : redisCacheConfiguration, cacheConstants.getMap());
            }
        };
    }

    @Bean
    @Primary
    public CacheInterceptor kerayCacheInterceptor(CacheOperationSource cacheOperationSource) {
        CacheInterceptor interceptor = new KerayCacheAspectSupport();
        interceptor.configure(this.errorHandler, this.keyGenerator, this.cacheResolver, this.cacheManager);
        interceptor.setCacheOperationSource(cacheOperationSource);
        return interceptor;
    }


    private RedisCacheConfiguration createConfiguration(CacheProperties cacheProperties, RedisTemplate cacheRedisRedisTemplate) {
        CacheProperties.Redis redisProperties = cacheProperties.getRedis();
        var config = RedisCacheConfiguration.defaultCacheConfig();
        config = config.serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(cacheRedisRedisTemplate.getValueSerializer()));
        if (redisProperties.getTimeToLive() != null) {
            config = config.entryTtl(redisProperties.getTimeToLive());
        }
        if (redisProperties.getKeyPrefix() != null) {
            config = config.computePrefixWith(name -> redisProperties.getKeyPrefix() + ":" + name + ":");
        } else {
            config = config.computePrefixWith(name -> name + ":");
        }
        if (!redisProperties.isCacheNullValues()) {
            config = config.disableCachingNullValues();
        }
        if (!redisProperties.isUseKeyPrefix()) {
            config = config.disableKeyPrefix();
        }
        return config;
    }

}
