package com.keray.common.keray.factory;

import com.keray.common.handler.ServletInvocableHandlerPipeline;
import com.keray.common.keray.KerayServletInvocableHandlerMethod;
import com.keray.common.resolver.KerayHandlerMethodArgumentResolverConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.annotation.ServletInvocableHandlerMethod;

import javax.annotation.Resource;

/**
 * @author by keray
 * date:2021/4/1 4:26 下午
 */
@Configuration
public class DefaultServletInvocableHandlerMethodFactory implements ServletInvocableHandlerMethodFactory {

    @Resource
    @Lazy
    private KerayHandlerMethodArgumentResolverConfig kerayHandlerMethodArgumentResolverConfig;


    @Override
    public ServletInvocableHandlerMethod create(HandlerMethod handlerMethod, ServletInvocableHandlerPipeline[] handlers) {
        return new KerayServletInvocableHandlerMethod(handlerMethod, handlers, kerayHandlerMethodArgumentResolverConfig);
    }
}
