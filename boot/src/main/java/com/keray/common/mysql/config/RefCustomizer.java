package com.keray.common.mysql.config;

import com.baomidou.mybatisplus.autoconfigure.ConfigurationCustomizer;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.keray.common.mysql.config.core.MybatisMapperProxy;
import com.keray.common.mysql.config.core.MybatisMapperRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Method;

/**
 * 反射修改MybatisConfiguration.mybatisMapperRegistry
 */
@Slf4j
@Configuration
@ConditionalOnClass(ConfigurationCustomizer.class)
public class RefCustomizer implements ConfigurationCustomizer {
    @Override
    public void customize(MybatisConfiguration configuration) {
        try {
            /**
             * 改变这个主要是为了拦截数据库的insert update时修改时间 设置id
             * {@link MybatisMapperProxy#invoke(Object, Method, Object[])}
             * */
            log.info("变更mybatis-plus的MybatisMapperRegistry对象");
            var refObj = new MybatisMapperRegistry(configuration);
            var field = MybatisConfiguration.class.getDeclaredField("mybatisMapperRegistry");
            field.setAccessible(true);
            field.set(configuration, refObj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
