package com.keray.common.file;

import cn.hutool.core.bean.BeanUtil;
import com.aliyun.oss.model.CannedAccessControlList;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author by keray
 * date:2020/9/19 9:51 上午
 */

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty("plugins.aliyun.oss.endpoint")
@ConfigurationProperties(prefix = "plugins.aliyun.oss", ignoreUnknownFields = false)
@Getter
@Setter
public class CommonOssConfig implements OssConfig {

    private String endpoint;

    private String extranetEndpoint;

    private String bucket;

    private String cdnBucket;

    private String diskBucket;

    private String basePath;

    private String region;

    private Integer pollCount = 10;
    private Integer pollMax = 100;

    private CannedAccessControlList acl;

    public CommonOssConfig copy() {
        CommonOssConfig config = new CommonOssConfig();
        BeanUtil.copyProperties(this, config);
        return config;
    }

}
