package com.keray.common.diamond;

import lombok.SneakyThrows;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;

import java.util.LinkedList;

public class DiamondBeanPostProcessor implements BeanPostProcessor, FactoryBean<DiamondBeanPostProcessor> {
    private DiamondManger manger;

    private final LinkedList<Object> beans = new LinkedList<>();


    public DiamondBeanPostProcessor(ConfigurableBeanFactory configurableBeanFactory) {
        configurableBeanFactory.addBeanPostProcessor(this);
    }

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

    @Override
    public DiamondBeanPostProcessor getObject() throws Exception {
        return this;
    }

    @Override
    public Class<?> getObjectType() {
        return DiamondBeanPostProcessor.class;
    }
}
