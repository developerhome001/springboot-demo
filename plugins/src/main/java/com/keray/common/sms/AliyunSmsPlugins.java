package com.keray.common.sms;

import com.alibaba.fastjson.JSON;
import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.teaopenapi.models.Config;
import com.keray.common.AliyunConfig;
import com.keray.common.AliyunPlugins;
import com.keray.common.IPlugins;
import com.keray.common.threadpool.SysThreadPool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * @author by keray
 * date:2021/4/8 5:23 下午
 */
@Service
@Slf4j
@ConditionalOnBean(AliyunSmsConfig.class)
public class AliyunSmsPlugins extends AliyunPlugins implements SmsPlugins, IPlugins {

    private final Client client;

    private final AliyunSmsConfig smsConfig;

    public AliyunSmsPlugins(AliyunConfig aliyunConfig, AliyunSmsConfig smsConfig) throws Exception {
        super(aliyunConfig);
        this.smsConfig = smsConfig;
        Config config = new Config()
                .setAccessKeyId(aliyunConfig.getAccessKeyId())
                .setAccessKeySecret(aliyunConfig.getAccessKeySecret());
        // 访问的域名
        config.setEndpoint(smsConfig.getEndpoint());
        client = new Client(config);
    }


    @Override
    public SmsStatus send(String phone, Map<String, Object> data) {
        return send(Collections.singletonList(phone), data).get(0).getStatus();
    }

    @Override
    public Future<SmsStatus> sendAsync(String phone, Map<String, Object> data) {
        return SysThreadPool.submit(() -> sendAsync(Collections.singletonList(phone), data).get(0).get().getStatus());
    }

    @Override
    public void sendCall(String phone, Map<String, Object> data, SmsSendCallback callback) {
        sendCall(Collections.singletonList(phone), data, callback);
    }

    @Override
    public List<SmsSendResult> send(List<String> phoneList, Map<String, Object> data) {
        List<SmsSendResult> results = new ArrayList<>(phoneList.size() + 1);
        List<Future<SmsSendResult>> future = sendAsync(phoneList, data);
        for (int i = 0; i < future.size(); i++) {
            try {
                SmsSendResult result = future.get(i).get();
                results.add(result);
            } catch (InterruptedException | ExecutionException e) {
                log.error("短信发送失败 mobile:" + phoneList.get(i), e);
                results.add(new SmsSendResult(phoneList.get(i), SmsStatus.fail));
            }
        }
        return results;
    }

    @Override
    public List<Future<SmsSendResult>> sendAsync(List<String> phoneList, Map<String, Object> data) {
        try {
            return SysThreadPool.submit(() -> {
                List<Future<SmsSendResult>> r = null;
                try {
                    SendSmsRequest request = new SendSmsRequest()
                            .setPhoneNumbers(String.join(",", phoneList))
                            .setSignName(data.containsKey("SignName") ? data.get("SignName").toString() : smsConfig.getSignName())
                            .setTemplateCode(data.get("TemplateCode").toString())
                            .setTemplateParam(data.get("TemplateParam") instanceof String ? data.get("TemplateParam").toString() : JSON.toJSONString(data.get("TemplateParam")));
                    SendSmsResponse response = client.sendSms(request);
                    if ("OK".equals(response.getBody().getCode())) {
                        r = phoneList.stream()
                                .map(p -> CompletableFuture.supplyAsync(() -> new SmsSendResult(p, SmsStatus.sendSuccess)))
                                .collect(Collectors.toList());
                    } else {
                        log.warn("短信发送失败：{}", response.getBody());
                    }
                } catch (Exception e) {
                    log.error("短信验证码发送失败：", e);
                }
                if (r == null) {
                    r = phoneList.stream()
                            .map(p -> CompletableFuture.supplyAsync(() -> new SmsSendResult(p, SmsStatus.fail)))
                            .collect(Collectors.toList());
                }
                return r;
            }).get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("not go there", e);
            return null;
        }
    }

    @Override
    public void sendCall(List<String> phoneList, Map<String, Object> data, SmsSendCallback callback) {
        List<Future<SmsSendResult>> future = sendAsync(phoneList, data);
        future.forEach(f -> {
            try {
                SmsSendResult result = f.get();
                callback.applay(result.getStatus(), result.getPhone());
            } catch (InterruptedException | ExecutionException e) {
                log.error("短信验--not go there", e);
            }
        });
    }
}
