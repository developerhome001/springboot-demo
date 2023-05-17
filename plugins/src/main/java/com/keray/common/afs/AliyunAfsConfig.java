package com.keray.common.afs;

import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(value = "plugins.aliyun.afs.regionid")
@ConfigurationProperties(prefix = "plugins.aliyun.afs")
@Data
public class AliyunAfsConfig {

    private String regionid;
}
