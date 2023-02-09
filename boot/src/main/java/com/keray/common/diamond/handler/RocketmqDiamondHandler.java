package com.keray.common.diamond.handler;

import cn.hutool.core.map.MapUtil;
import com.alibaba.fastjson.JSON;
import com.keray.common.diamond.DiamondManger;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import javax.annotation.Resource;

@Slf4j
@ConditionalOnClass(RocketMQTemplate.class)
@Configuration(proxyBeanMethods = false)
@RocketMQMessageListener(topic = RocketmqDiamondHandler.MQ_EX, consumerGroup = RocketmqDiamondHandler.MQ_RT, messageModel = MessageModel.BROADCASTING)
public class RocketmqDiamondHandler implements DiamondHandler, RocketMQListener<String> {

    public final static String MQ_EX = "keray_diamond_topic";
    public final static String MQ_RT = "keray_diamond_group";
    @Resource
    @Lazy
    private DiamondManger diamondManger;

    @Resource
    private RocketMQTemplate rocketMQTemplate;


    @Override
    public void handler(String key, String value) {
        rocketMQTemplate.convertAndSend(MQ_EX, JSON.toJSON(MapUtil.builder()
                .put("key", key)
                .put("value", value)
                .build()));
    }

    @Override
    public void onMessage(String s) {
        var object = JSON.parseObject(s);
        var key = object.getString("key");
        var value = object.getString("value");
        try {
            diamondManger.diamondChange(key, value);
        } catch (Exception e) {
            log.error("消息通知失败", e);
        }
    }
}
