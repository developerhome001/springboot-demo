package com.keray.common.cache;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.AbstractCachingConfiguration;
import org.springframework.cache.annotation.AnnotationCacheOperationSource;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.annotation.ProxyCachingConfiguration;
import org.springframework.cache.config.CacheManagementConfigUtils;
import org.springframework.cache.ehcache.EhCacheManagerUtils;
import org.springframework.cache.interceptor.BeanFactoryCacheOperationSourceAdvisor;
import org.springframework.cache.interceptor.CacheInterceptor;
import org.springframework.cache.interceptor.CacheOperationSource;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;

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
    public RedisCacheManager cacheManager(CacheProperties cacheProperties, @Qualifier("cacheRedisRedisTemplate") RedisTemplate cacheRedisRedisTemplate, ResourceLoader resourceLoader) {
        Resource resource = cacheProperties.getEhcache().getConfig();
        net.sf.ehcache.config.Configuration conf = null;
        if (resource != null) {
            conf = EhCacheManagerUtils.parseConfiguration(resource);
        }
        RedisCacheWriter redisCacheWriter = new ERedisCacheWriter(cacheRedisRedisTemplate.getConnectionFactory(), conf == null ? null : conf.getCacheConfigurations());
        var redisCacheConfiguration = createConfiguration(cacheProperties, resourceLoader)
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(cacheRedisRedisTemplate.getValueSerializer()));
        return new RedisCacheManager(redisCacheWriter, redisCacheConfiguration);
    }

    @Bean
    @Primary
    public CacheInterceptor kerayCacheInterceptor(CacheOperationSource cacheOperationSource) {
        CacheInterceptor interceptor = new KerayCacheAspectSupport();
        interceptor.configure(this.errorHandler, this.keyGenerator, this.cacheResolver, this.cacheManager);
        interceptor.setCacheOperationSource(cacheOperationSource);
        return interceptor;
    }


    private RedisCacheConfiguration createConfiguration(
            CacheProperties cacheProperties, ResourceLoader resourceLoader) {
        CacheProperties.Redis redisProperties = cacheProperties.getRedis();
        org.springframework.data.redis.cache.RedisCacheConfiguration config = org.springframework.data.redis.cache.RedisCacheConfiguration
                .defaultCacheConfig();
        config = config.serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(new JdkSerializationRedisSerializer(resourceLoader.getClassLoader())));
        if (redisProperties.getTimeToLive() != null) {
            config = config.entryTtl(redisProperties.getTimeToLive());
        }
        if (redisProperties.getKeyPrefix() != null) {
            config = config.prefixCacheNameWith(redisProperties.getKeyPrefix());
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
