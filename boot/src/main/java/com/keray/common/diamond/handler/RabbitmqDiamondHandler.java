package com.keray.common.diamond.handler;

import cn.hutool.core.map.MapUtil;
import com.alibaba.fastjson.JSON;
import com.keray.common.diamond.DiamondManger;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import javax.annotation.Resource;

@ConditionalOnClass(RabbitTemplate.class)
@Configuration
public class RabbitmqDiamondHandler implements DiamondHandler {

    private final static String MQ_EX = "keray.diamond.ex";
    private final static String MQ_RT = "keray.diamond.#";

    @Resource
    @Lazy
    private DiamondManger diamondManger;

    @Resource
    private RabbitTemplate rabbitTemplate;


    @Override
    public void handler(String key, String value) {
        rabbitTemplate.convertAndSend(MQ_EX, MQ_RT, new Message(JSON.toJSONBytes(MapUtil.builder()
                .put("key", key)
                .put("value", value)
                .build())));

    }

    @RabbitListener(bindings = {@QueueBinding(value = @Queue(), exchange = @Exchange(value = MQ_EX, type = ExchangeTypes.FANOUT), key = MQ_RT)}, ackMode = "AUTO")
    public void suffixChangeMq(Message message) throws Exception {
        var object = JSON.parseObject(message.getBody());
        var key = object.getString("key");
        var value = object.getString("value");
        diamondManger.diamondChange(key, value);
    }

}
