package com.keray.common;

import com.aliyun.oss.model.CannedAccessControlList;
import com.keray.common.file.CommonOssConfig;
import com.keray.common.file.OssPlugins;
import com.keray.common.sms.AliyunSmsConfig;
import com.keray.common.sms.AliyunSmsPlugins;
import com.keray.common.video.AliyunVideoConfig;
import com.keray.common.video.AliyunVideoPlugins;
import com.keray.common.video.VideoPlugins;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * @author by keray
 * date:2021/6/28 10:43 上午
 */
@Configuration
public class AliyunPluginsAuto {
    /**
     * 私有文件需鉴权
     *
     * @param aliyunConfig
     * @param ossConfig
     * @return
     */
    @Primary
    @Bean
    @ConditionalOnBean(CommonOssConfig.class)
    public OssPlugins middleOssPlugins(AliyunConfig aliyunConfig, CommonOssConfig ossConfig, @Qualifier("redisTemplate") RedisTemplate redisTemplate) {
        CommonOssConfig config = ossConfig.copy();
        config.setBasePath("middle/" + ossConfig.getBasePath());
        config.setAcl(CannedAccessControlList.Private);
        return new OssPlugins(aliyunConfig, config, redisTemplate);
    }

    /**
     * 无需鉴权资源
     *
     * @param aliyunConfig
     * @param ossConfig
     * @return
     */

    @Bean
    @ConditionalOnBean(CommonOssConfig.class)
    public OssPlugins cdnOssPlugins(AliyunConfig aliyunConfig, CommonOssConfig ossConfig, @Qualifier("redisTemplate") RedisTemplate redisTemplate) {
        CommonOssConfig config = ossConfig.copy();
        config.setBucket(ossConfig.getCdnBucket());
        config.setBasePath("cdn/" + ossConfig.getBasePath());
        config.setAcl(CannedAccessControlList.PublicRead);
        return new OssPlugins(aliyunConfig, config, redisTemplate);
    }

    /**
     * <p>
     * <h3>作者 keray</h3>
     * <h3>时间： 2021/4/22 9:01 下午</h3>
     * </p>
     * 归档类文件，极低的访问频率
     *
     * @return <p> {@link OssPlugins} </p>
     * @throws
     */

    @Bean
    @ConditionalOnBean(CommonOssConfig.class)
    public OssPlugins diskOssPlugins(AliyunConfig aliyunConfig, CommonOssConfig ossConfig, @Qualifier("redisTemplate") RedisTemplate redisTemplate) {
        CommonOssConfig config = ossConfig.copy();
        config.setBucket(ossConfig.getDiskBucket());
        config.setBasePath("disk/" + ossConfig.getBasePath());
        config.setAcl(CannedAccessControlList.Private);
        return new OssPlugins(aliyunConfig, config, redisTemplate);
    }

    @Bean
    @ConditionalOnBean(AliyunVideoConfig.class)
    @ConditionalOnMissingBean(AliyunVideoPlugins.class)
    public VideoPlugins aliVideoPlugins(AliyunConfig aliyunConfig, AliyunVideoConfig videoConfig, @Qualifier("redisTemplate") RedisTemplate redisTemplate) {
        return new AliyunVideoPlugins(aliyunConfig, videoConfig, redisTemplate);
    }

    @Bean
    @ConditionalOnBean(AliyunSmsConfig.class)
    @ConditionalOnMissingBean(AliyunSmsPlugins.class)
    public AliyunSmsPlugins aliyunSmsPlugins(AliyunConfig aliyunConfig, AliyunSmsConfig aliyunSmsConfig) throws Exception {
        return new AliyunSmsPlugins(aliyunConfig, aliyunSmsConfig);
    }
}
