package com.keray.common.handler;

import com.keray.common.ServletInvocableHandlerMethodFactory;
import com.keray.common.ServletInvocableHandlerMethodHandler;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcRegistrations;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.util.ProxyUtils;
import org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;

@Configuration(proxyBeanMethods = false)
@Primary
public class KerayWebMvcRegistrations implements WebMvcRegistrations, BeanPostProcessor {


    private ServletInvocableHandlerMethodHandler[] handlers = new ServletInvocableHandlerMethodHandler[]{};

    @Resource
    private ServletInvocableHandlerMethodFactory servletInvocableHandlerMethodFactory;


    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        var clazz = ProxyUtils.getUserClass(bean);
        if (ServletInvocableHandlerMethodHandler.class.isAssignableFrom(clazz)) {
            var list = new LinkedList<>(Arrays.asList(handlers));
            list.add((ServletInvocableHandlerMethodHandler) bean);
            var array = list.toArray(new ServletInvocableHandlerMethodHandler[]{});
            Arrays.sort(array, Comparator.comparing(ServletInvocableHandlerMethodHandler::getOrder));
            handlers = array;
        }
        return BeanPostProcessor.super.postProcessAfterInitialization(bean, beanName);
    }

    @Override
    public RequestMappingHandlerMapping getRequestMappingHandlerMapping() {
        return WebMvcRegistrations.super.getRequestMappingHandlerMapping();
    }

    @Override
    public RequestMappingHandlerAdapter getRequestMappingHandlerAdapter() {
        return new IRequestMappingHandlerAdapter(servletInvocableHandlerMethodFactory, handlers);
    }

    @Override
    public ExceptionHandlerExceptionResolver getExceptionHandlerExceptionResolver() {
        return WebMvcRegistrations.super.getExceptionHandlerExceptionResolver();
    }
}
