package com.keray.common.sms;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author by keray
 * date:2021/4/8 5:53 下午
 */
@Configuration
@ConditionalOnProperty("plugins.aliyun.sms.endpoint")
@ConfigurationProperties(prefix = "plugins.aliyun.sms", ignoreUnknownFields = false)
@Getter
@Setter
public class AliyunSmsConfig {
    private String endpoint;
    private String signName;
}
