package com.keray.common.keray;

import com.keray.common.handler.ServletInvocableHandlerPipeline;
import com.keray.common.keray.factory.ServletInvocableHandlerMethodFactory;
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


    private final List<ServletInvocableHandlerPipeline> handlers = Collections.synchronizedList(new ArrayList<>(8));

    @Resource
    private ServletInvocableHandlerMethodFactory servletInvocableHandlerMethodFactory;


    public KerayWebMvcRegistrations(ApplicationContext context) {
        handlers.addAll(context.getBeansOfType(ServletInvocableHandlerPipeline.class).values());
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (ServletInvocableHandlerPipeline.class.isAssignableFrom(bean.getClass())) {
            handlers.add((ServletInvocableHandlerPipeline) bean);
        }
        return BeanPostProcessor.super.postProcessAfterInitialization(bean, beanName);
    }

    @Override
    public RequestMappingHandlerAdapter getRequestMappingHandlerAdapter() {
        return new KerayRequestMappingHandlerAdapter(servletInvocableHandlerMethodFactory, handlers);
    }

}
