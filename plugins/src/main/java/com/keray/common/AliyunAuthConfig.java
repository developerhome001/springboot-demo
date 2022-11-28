package com.keray.common;

import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author by keray
 * date:2020/9/24 9:58 下午
 */
@Configuration
@ConditionalOnProperty(value = "plugins.aliyun.auth.role-arn")
@ConfigurationProperties(prefix = "plugins.aliyun.auth")
@Data
public class AliyunAuthConfig {
    private String roleArn;
    private String endpoint;
}
