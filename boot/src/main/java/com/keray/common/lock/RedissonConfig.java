package com.keray.common.lock;

import com.keray.common.config.KerayRedisConfig;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.spring.data.connection.RedissonConnectionFactory;
import org.redisson.spring.starter.RedissonAutoConfiguration;
import org.redisson.spring.starter.RedissonAutoConfigurationCustomizer;
import org.redisson.spring.starter.RedissonProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.util.ReflectionUtils;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.*;

@Configuration
@ConditionalOnClass(RedissonAutoConfiguration.class)
@Slf4j
public class RedissonConfig {

    private static final String REDIS_PROTOCOL_PREFIX = "redis://";
    private static final String REDISS_PROTOCOL_PREFIX = "rediss://";
    private final KerayRedisConfig kerayRedisConfig;
    private final RedisProperties redisProperties;

    private final RedissonProperties redissonProperties;

    @javax.annotation.Resource
    private ApplicationContext ctx;

    private final List<RedissonAutoConfigurationCustomizer> redissonAutoConfigurationCustomizers;

    public RedissonConfig(KerayRedisConfig kerayRedisConfig, RedisProperties redisProperties, RedissonProperties redissonProperties, List<RedissonAutoConfigurationCustomizer> redissonAutoConfigurationCustomizers) {
        this.kerayRedisConfig = kerayRedisConfig;
        this.redisProperties = redisProperties;
        this.redissonProperties = redissonProperties;
        this.redissonAutoConfigurationCustomizers = redissonAutoConfigurationCustomizers;
    }

    // redisson
    @Bean(name = "redissonClient")
    public RedissonClient redisson() {
        log.info("注入redis redisson库：{}", kerayRedisConfig.getRedissonDb());
        Config config;
        Duration timeoutValue = redisProperties.getTimeout();
        int timeout;
        if (null == timeoutValue) {
            timeout = 10000;
        } else {
            timeout = (int) timeoutValue.toMillis();
        }
        if (redisProperties.getSentinel() != null) {
            var nodes = convert(redisProperties.getSentinel().getNodes());
            config = new Config();
            config.useSentinelServers()
                    .setMasterName(redisProperties.getSentinel().getMaster())
                    .addSentinelAddress(nodes)
                    .setDatabase(redisProperties.getDatabase())
                    .setConnectTimeout(timeout)
                    .setPassword(redisProperties.getPassword());
        } else if (redisProperties.getCluster() != null) {
            String[] nodes = convert(redisProperties.getCluster().getNodes());
            config = new Config();
            config.useClusterServers()
                    .addNodeAddress(nodes)
                    .setConnectTimeout(timeout)
                    .setPassword(redisProperties.getPassword());
        } else {
            config = new Config();
            String prefix = REDIS_PROTOCOL_PREFIX;
            if (redisProperties.isSsl()) {
                prefix = REDISS_PROTOCOL_PREFIX;
            }
            config.useSingleServer()
                    .setAddress(prefix + redisProperties.getHost() + ":" + redisProperties.getPort())
                    .setConnectTimeout(timeout)
                    .setDatabase(kerayRedisConfig.getRedissonDb())
                    .setPassword(redisProperties.getPassword());
        }
        if (redissonAutoConfigurationCustomizers != null) {
            for (RedissonAutoConfigurationCustomizer customizer : redissonAutoConfigurationCustomizers) {
                customizer.customize(config);
            }
        }
        return Redisson.create(config);
    }

    private String[] convert(List<String> nodesObject) {
        List<String> nodes = new ArrayList<>(nodesObject.size());
        for (String node : nodesObject) {
            if (!node.startsWith(REDIS_PROTOCOL_PREFIX) && !node.startsWith(REDISS_PROTOCOL_PREFIX)) {
                nodes.add(REDIS_PROTOCOL_PREFIX + node);
            } else {
                nodes.add(node);
            }
        }
        return nodes.toArray(new String[0]);
    }

    @Bean
    public RedissonConnectionFactory redissonConnectionFactory(@Qualifier("redissonClient") RedissonClient redisson) {
        return new RedissonConnectionFactory(redisson);
    }

    @Bean
    public RedissonLock redissonLock(RedissonClient redissonClient) {
        return new RedissonLock(redissonClient);
    }

}
