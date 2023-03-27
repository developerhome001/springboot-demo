package com.keray.common.mysql.config;

import com.baomidou.mybatisplus.autoconfigure.ConfigurationCustomizer;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.keray.common.mysql.config.core.MybatisMapperRegistry;
import com.keray.common.service.mapper.MybatisPlusCacheMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

/**
 * 反射修改MybatisConfiguration.mybatisMapperRegistry
 */
@Slf4j
@Configuration
public class RefCustomizer implements ConfigurationCustomizer {
    @Override
    public void customize(MybatisConfiguration configuration) {
        try {
            log.info("变更mybatis-plus的MybatisMapperRegistry对象");
            var refObj = new MybatisMapperRegistry(configuration);
            var field = MybatisConfiguration.class.getDeclaredField("mybatisMapperRegistry");
            field.setAccessible(true);
            field.set(configuration, refObj);
            refObj.addMapper(MybatisPlusCacheMapper.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
