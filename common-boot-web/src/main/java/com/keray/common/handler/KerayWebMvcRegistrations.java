package com.keray.common.handler;

import cn.hutool.core.collection.CollUtil;
import com.keray.common.ServletInvocableHandlerMethodFactory;
import com.keray.common.ServletInvocableHandlerMethodHandler;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcRegistrations;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;

@Configuration(proxyBeanMethods = false)
@Primary
public class KerayWebMvcRegistrations implements WebMvcRegistrations {


    ServletInvocableHandlerMethodHandler[] handlers = null;

    @Resource
    private ServletInvocableHandlerMethodFactory servletInvocableHandlerMethodFactory;

    @Resource
    private ApplicationContext applicationContext;

    @PostConstruct
    public void initHandler() {
        Collection<ServletInvocableHandlerMethodHandler> handlerMethodHandlers = applicationContext
                .getBeansOfType(ServletInvocableHandlerMethodHandler.class).values();
        if (CollUtil.isNotEmpty(handlerMethodHandlers)) {
            ServletInvocableHandlerMethodHandler[] array = handlerMethodHandlers.toArray(new ServletInvocableHandlerMethodHandler[]{});
            Arrays.sort(array, Comparator.comparing(ServletInvocableHandlerMethodHandler::getOrder));
            handlers = array;
        }
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
