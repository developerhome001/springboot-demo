package com.keray.common.mysql.config;

import com.baomidou.mybatisplus.autoconfigure.ConfigurationCustomizer;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.keray.common.mysql.config.core.MybatisMapperRegistry;
import org.springframework.context.annotation.Configuration;

/**
 * 反射修改MybatisConfiguration.mybatisMapperRegistry
 */
@Configuration
public class RefCustomizer implements ConfigurationCustomizer {
    @Override
    public void customize(MybatisConfiguration configuration) {
        try {
            var field = MybatisConfiguration.class.getDeclaredField("mybatisMapperRegistry");
            field.setAccessible(true);
            field.set(configuration, new MybatisMapperRegistry(configuration));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
