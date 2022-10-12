package com.keray.common.video;

import com.aliyun.teaopenapi.models.Config;
import com.aliyun.vod20170321.Client;
import com.aliyun.vod20170321.models.CreateUploadVideoRequest;
import com.keray.common.AliyunAuthPlugins;
import com.keray.common.AliyunConfig;
import com.keray.common.AliyunPlugins;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author by keray
 * date:2021/6/28 10:32 上午
 */
@Slf4j
@Service
@ConditionalOnBean(AliyunVideoConfig.class)
public class AliyunVideoPlugins extends AliyunPlugins implements VideoPlugins {

    protected Client client;

    private final AliyunVideoConfig videoConfig;

    private final RedisTemplate<String, String> redisTemplate;

    public AliyunVideoPlugins(AliyunConfig aliyunConfig, AliyunVideoConfig videoConfig, RedisTemplate<String, String> redisTemplate) {
        super(aliyunConfig);
        this.videoConfig = videoConfig;
        this.redisTemplate = redisTemplate;
    }


    @SneakyThrows
    private synchronized Client createClient() {
        if (client == null) {
            client = new Client(new Config()
                    .setAccessKeyId(aliyunConfig.getAccessKeyId())
                    .setAccessKeySecret(aliyunConfig.getAccessKeySecret())
                    .setEndpoint(videoConfig.getEndpoint())
            );
        }
        return client;
    }

    @Override
    public Object auth(String videoFileName, String title) throws Exception {
        return createClient().createUploadVideo(new CreateUploadVideoRequest()
                .setFileName(videoFileName)
                .setTitle(title)
        );
    }
}
