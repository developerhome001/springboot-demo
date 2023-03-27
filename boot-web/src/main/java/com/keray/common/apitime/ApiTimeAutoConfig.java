package com.keray.common.apitime;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
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
