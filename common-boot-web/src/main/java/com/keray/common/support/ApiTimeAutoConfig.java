package com.keray.common.support;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(value = "keray.api.time", havingValue = "true")
public class ApiTimeAutoConfig {

    @Bean
    @ConditionalOnMissingBean(ApiTimeRecordDb.class)
    public ApiTimeRecordDb defaultApiTimeRecordDao() {
        return new DefaultApiTimeRecordDao();
    }

    @Bean
    public ApiTimeInterceptor apiTimeInterceptor() {
        return new ApiTimeInterceptor();
    }

}
