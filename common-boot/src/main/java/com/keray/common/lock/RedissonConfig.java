package com.keray.common.lock;

import cn.hutool.core.util.StrUtil;
import com.keray.common.config.RedisConfig;
import com.keray.common.lock.RedissonLock;
import com.keray.common.utils.UUIDUtil;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.config.Config;
import org.redisson.config.TransportMode;
import org.redisson.spring.data.connection.RedissonConnectionFactory;
import org.redisson.spring.starter.RedissonAutoConfiguration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
@Slf4j
public class RedissonConfig {

    private final RedisConfig redisConfig;

    public RedissonConfig(RedisConfig redisConfig) {
        this.redisConfig = redisConfig;
    }

    // redisson
    @Bean(name = "redissonClient")
    @ConditionalOnClass(RedissonAutoConfiguration.class)
    public RedissonClient redissonClient(RedisProperties redisProperties) throws IOException {
        log.info("注入redis redisson库：{}", redisConfig.getRedissonDb());
        Config config = new Config();
        config.useSingleServer().setAddress(StrUtil.format("redis://{}:{}", redisProperties.getHost(), redisProperties.getPort()))
                .setPassword(redisProperties.getPassword())
                .setDatabase(redisConfig.getRedissonDb())
                .setTimeout(30_000);
        config.setTransportMode(TransportMode.NIO);
        config.setCodec(new JsonJacksonCodec(redisConfig.getOm()));
        return Redisson.create(config);
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
