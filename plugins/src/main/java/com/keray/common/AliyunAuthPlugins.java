package com.keray.common;

import cn.hutool.core.map.MapUtil;
import com.alibaba.fastjson.JSON;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.auth.sts.AssumeRoleRequest;
import com.aliyuncs.auth.sts.AssumeRoleResponse;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.http.MethodType;
import com.aliyuncs.profile.DefaultProfile;
import com.aliyuncs.profile.IClientProfile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * @author by keray
 * date:2021/4/23 9:51 上午
 */
@Service
@ConditionalOnBean(AliyunConfig.class)
public class AliyunAuthPlugins extends AliyunPlugins {


    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${spring.profiles.active}")
    private String env;

    private final AliyunAuthConfig aliyunAuthConfig;

    public AliyunAuthPlugins(AliyunConfig aliyunConfig, AliyunAuthConfig aliyunAuthConfig) {
        super(aliyunConfig);
        this.aliyunAuthConfig = aliyunAuthConfig;
    }

    /**
     * <p>
     * <h3>作者 keray</h3>
     * <h3>时间： 2021/4/23 10:13 上午</h3>
     * action配置https://help.aliyun.com/document_detail/100680.htm?spm=a2c4g.11186623.2.10.3260606cptYWbF#section-lda-vgc-p09
     * </p>
     *
     * @return <p> {@link AssumeRoleResponse} </p>
     */
    public AssumeRoleResponse stsAuth(List<String> action, List<String> resource, Long DurationSeconds) throws ClientException {
        String AccessKeyId = aliyunConfig.getAccessKeyId();
        String accessKeySecret = aliyunConfig.getAccessKeySecret();
        String roleArn = aliyunAuthConfig.getRoleArn();
        String roleSessionName = applicationName + env;
        String policy = JSON.toJSONString(MapUtil.builder()
                .put("Version", "1")
                .put("Statement",
                        Collections.singletonList(
                                MapUtil.builder()
                                        .put("Effect", "Allow")
                                        .put("Action", action)
                                        .put("Resource", resource)
                                        .build()
                        )
                ).build());
        DefaultProfile.addEndpoint("", "", "Sts", aliyunAuthConfig.getEndpoint());
        IClientProfile profile = DefaultProfile.getProfile("", AccessKeyId, accessKeySecret);
        DefaultAcsClient client = new DefaultAcsClient(profile);
        final AssumeRoleRequest request = new AssumeRoleRequest();
        request.setMethod(MethodType.POST);
        request.setRoleArn(roleArn);
        request.setRoleSessionName(roleSessionName);
        request.setPolicy(policy);
        request.setDurationSeconds(DurationSeconds); // 设置凭证有效时间
        return client.getAcsResponse(request);
    }

}
