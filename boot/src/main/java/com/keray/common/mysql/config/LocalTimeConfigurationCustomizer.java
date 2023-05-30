package com.keray.common.mysql.config;

import com.baomidou.mybatisplus.autoconfigure.ConfigurationCustomizer;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.keray.common.mysql.handler.LocalDateTimeTypeHandler;
import com.keray.common.mysql.handler.LocalDateTypeHandler;
import com.keray.common.mysql.handler.LocalTimeTypeHandler;
import com.keray.common.mysql.handler.StringEncryptionHandler;
import org.apache.ibatis.type.JdbcType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@ConditionalOnClass(ConfigurationCustomizer.class)
@Configuration
public class LocalTimeConfigurationCustomizer implements ConfigurationCustomizer {
    @Override
    public void customize(MybatisConfiguration configuration) {
        configuration.getTypeHandlerRegistry().register(LocalDateTime.class, LocalDateTimeTypeHandler.class);
        configuration.getTypeHandlerRegistry().register(LocalDate.class, LocalDateTypeHandler.class);
        configuration.getTypeHandlerRegistry().register(LocalTime.class, LocalTimeTypeHandler.class);
        configuration.getTypeHandlerRegistry().register(String.class, JdbcType.VARCHAR, StringEncryptionHandler.class);
        configuration.getTypeHandlerRegistry().register(String.class, StringEncryptionHandler.class);
    }
}
