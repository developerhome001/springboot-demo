package com.keray.common.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * redis配置
 *
 * @author keray
 */
@Configuration("kerayRedisConfig")
@ConditionalOnClass(RedisConnectionFactory.class)
@ConfigurationProperties("spring.redis")
@Slf4j
public class KerayRedisConfig {

    @Getter
    private final ObjectMapper om;

    @Setter
    @Getter
    private Integer cacheDb = 2;
    @Setter
    @Getter
    private Integer redissonDb = 3;

    @Setter
    @Getter
    private Integer importantDb = 4;


    private final Jackson2JsonRedisSerializer jacksonSeial = new Jackson2JsonRedisSerializer(Object.class);

    public KerayRedisConfig() {
        this.om = redisObjectMapper();
    }

    /**
     * @param redisConnectionFactory
     * @param redisProperties
     * @return
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean(name = "redisTemplate")
    public RedisTemplate redisTemplate(RedisConnectionFactory redisConnectionFactory, RedisProperties redisProperties) {
        log.info("注入redis基础库：{}", redisProperties.getDatabase());
        return redisTemplateFactory(redisConnectionFactory, redisProperties.getDatabase());
    }


    /**
     * @param redisConnectionFactory
     * @return
     */
    @Bean(name = "cacheRedisRedisTemplate")
    public RedisTemplate cacheRedisRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        log.info("注入redis缓存库：{}", cacheDb);
        return redisTemplateFactory(redisConnectionFactory, cacheDb);
    }


    /**
     * <p>
     * <h3>作者 keray</h3>
     * <h3>时间： 2019/11/28 5:32 PM</h3>
     * 重要的Redis数据  不允许清除
     * </p>
     *
     * @param redisConnectionFactory
     * @return <p> {@link RedisTemplate} </p>
     */
    @Bean(name = "persistenceRedisTemplate")
    public RedisTemplate persistenceRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        log.info("注入redis持久库：{}", importantDb);
        return redisTemplateFactory(redisConnectionFactory, importantDb);
    }


    public RedisTemplate redisTemplateFactory(RedisConnectionFactory factory, Integer database) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        if (factory instanceof LettuceConnectionFactory lf) {
            lf.setDatabase(database);
        }
        // 配置连接工厂
        template.setConnectionFactory(factory);
        jacksonSeial.setObjectMapper(om);
        // 值采用json序列化
        template.setValueSerializer(jacksonSeial);
        //使用StringRedisSerializer来序列化和反序列化redis的key值
        template.setKeySerializer(new StringRedisSerializer());
        // 设置hash key 和value序列化模式
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(jacksonSeial);
        template.afterPropertiesSet();
        return template;
    }


    /**
     * 默认日期时间格式
     */
    public static final String DEFAULT_DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    /**
     * 默认日期格式
     */
    public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";
    /**
     * 默认时间格式
     */
    public static final String DEFAULT_TIME_FORMAT = "HH:mm:ss";


    private JavaTimeModule javaModule() {

        JavaTimeModule javaTimeModule = new JavaTimeModule();
        javaTimeModule.addSerializer(LocalDateTime.class, new com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer(DateTimeFormatter.ofPattern(DEFAULT_DATE_TIME_FORMAT)));
        javaTimeModule.addSerializer(LocalDate.class, new com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer(DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT)));
        javaTimeModule.addSerializer(LocalTime.class, new com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer(DateTimeFormatter.ofPattern(DEFAULT_TIME_FORMAT)));
        javaTimeModule.addDeserializer(LocalDateTime.class, new com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer(DateTimeFormatter.ofPattern(DEFAULT_DATE_TIME_FORMAT)));
        javaTimeModule.addDeserializer(LocalDate.class, new com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer(DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT)));
        javaTimeModule.addDeserializer(LocalTime.class, new com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer(DateTimeFormatter.ofPattern(DEFAULT_TIME_FORMAT)));
        return javaTimeModule;
    }

    public ObjectMapper redisObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        //使用Jackson2JsonRedisSerializer来序列化和反序列化redis的value值（默认使用JDK的序列化方式）
        // 指定要序列化的域，field,get和set,以及修饰符范围，ANY是都有包括private和public
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        objectMapper.activateDefaultTyping(objectMapper.getPolymorphicTypeValidator(), ObjectMapper.DefaultTyping.NON_FINAL);
        //LocalDateTime系列序列化和反序列化模块，继承自jsr310，我们在这里修改了日期格式
        JavaTimeModule javaTimeModule = javaModule();
        objectMapper.registerModule(javaTimeModule);
        return objectMapper;
    }

}
