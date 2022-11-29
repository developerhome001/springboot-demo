package com.keray.common.keray;

import com.keray.common.handler.ServletInvocableHandlerMethodHandler;
import com.keray.common.keray.factory.ServletInvocableHandlerMethodFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcRegistrations;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Configuration(proxyBeanMethods = false)
@Primary
public class KerayWebMvcRegistrations implements WebMvcRegistrations, BeanPostProcessor {


    private final List<ServletInvocableHandlerMethodHandler> handlers = Collections.synchronizedList(new ArrayList<>(8));

    @Resource
    private ServletInvocableHandlerMethodFactory servletInvocableHandlerMethodFactory;


    public KerayWebMvcRegistrations(ApplicationContext context) {
        handlers.addAll(context.getBeansOfType(ServletInvocableHandlerMethodHandler.class).values());
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        var clazz = AopUtils.getTargetClass(bean);
        if (ServletInvocableHandlerMethodHandler.class.isAssignableFrom(clazz)) {
            handlers.add((ServletInvocableHandlerMethodHandler) bean);
        }
        return BeanPostProcessor.super.postProcessAfterInitialization(bean, beanName);
    }

    @Override
    public RequestMappingHandlerAdapter getRequestMappingHandlerAdapter() {
        return new KerayRequestMappingHandlerAdapter(servletInvocableHandlerMethodFactory, handlers);
    }

}
