package com.keray.common.keray;

import com.keray.common.handler.ExceptionServletInvocableHandlerPipeline;
import com.keray.common.handler.ServletInvocableHandlerPipeline;
import com.keray.common.keray.factory.ServletInvocableHandlerMethodFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcRegistrations;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

import javax.annotation.Resource;
import java.util.*;

@Configuration(proxyBeanMethods = false)
@Primary
public class KerayWebMvcRegistrations implements WebMvcRegistrations, BeanPostProcessor {


    private final Set<ServletInvocableHandlerPipeline> handlers = Collections.synchronizedSet(new LinkedHashSet<>());

    @Resource
    private ServletInvocableHandlerMethodFactory servletInvocableHandlerMethodFactory;


    public KerayWebMvcRegistrations(ApplicationContext context, ConfigurableBeanFactory configurableBeanFactory) {
        handlers.addAll(context.getBeansOfType(ServletInvocableHandlerPipeline.class).values());
        configurableBeanFactory.addBeanPostProcessor(this);
    }

    /**
     * 排序看注释
     * {@link ServletInvocableHandlerPipeline}
     */
    @Bean
    public ExceptionServletInvocableHandlerPipeline exceptionServletInvocableHandlerPipeline1() {
        var r = new ExceptionServletInvocableHandlerPipeline();
        r.setOrder(300);
        return r;
    }

    @Bean
    public ExceptionServletInvocableHandlerPipeline exceptionServletInvocableHandlerPipeline2() {
        var r = new ExceptionServletInvocableHandlerPipeline();
        r.setOrder(600);
        return r;
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
