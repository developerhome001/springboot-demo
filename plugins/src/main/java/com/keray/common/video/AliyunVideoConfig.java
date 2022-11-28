package com.keray.common.video;

import com.keray.common.AliyunConfig;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author by keray
 * date:2021/6/28 10:23 上午
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty("plugins.aliyun.video.endpoint")
@ConfigurationProperties(prefix = "plugins.aliyun.video", ignoreUnknownFields = false)
@Getter
@Setter
public class AliyunVideoConfig extends AliyunConfig {

    private String endpoint;
}
