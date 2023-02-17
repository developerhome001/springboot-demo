package com.keray.common.gateway.downgrade;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.util.ProxyUtils;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * 接口降级handler注册器
 */
@Configuration
public class ApiDowngradeRegister implements BeanPostProcessor {
    private final static Map<Class<? extends DowngradeHandler>, DowngradeHandler> REGISTER = new HashMap<>();

    @Resource
    private DefDowngradeHandler defDowngradeHandler;

    public ApiDowngradeRegister(ApplicationContext applicationContext) {
        applicationContext.getBeansOfType(DowngradeHandler.class).values().forEach(this::registerHandler);
    }


    /**
     * 注册接口降级处理器
     *
     * @param handler
     */
    public void registerHandler(DowngradeHandler handler) {
        REGISTER.put((Class<? extends DowngradeHandler>) ProxyUtils.getUserClass(handler), handler);
    }

    DowngradeHandler getRegister(Class<? extends DowngradeHandler> clazz) {
        // clazz设置为DowngradeHandler.class说明返回默认的处理
        if (clazz == DowngradeHandler.class) {
            return defDowngradeHandler;
        }
        return REGISTER.get(clazz);
    }


    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof DowngradeHandler dh) {
            registerHandler(dh);
        }
        return BeanPostProcessor.super.postProcessAfterInitialization(bean, beanName);
    }
}
