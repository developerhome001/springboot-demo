package com.keray.common.diamond;

import lombok.SneakyThrows;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedList;

@Configuration
public class DiamondAuto implements BeanPostProcessor {

    private DiamondManger manger;

    private final LinkedList<Object> beans = new LinkedList<>();

    @SneakyThrows
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (manger != null) {
            manger.handler(bean);
        } else {
            beans.add(bean);
        }
        if (bean instanceof DiamondManger dm) {
            manger = dm;
            beans.forEach(manger::handler);
            beans.clear();
        }
        return BeanPostProcessor.super.postProcessBeforeInitialization(bean, beanName);
    }

}
