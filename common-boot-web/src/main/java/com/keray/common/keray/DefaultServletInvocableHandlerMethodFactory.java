package com.keray.common.keray;

import com.keray.common.ServletInvocableHandlerMethodFactory;
import com.keray.common.ServletInvocableHandlerMethodHandler;
import com.keray.common.handler.IServletInvocableHandlerMethod;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.annotation.ServletInvocableHandlerMethod;

/**
 * @author by keray
 * date:2021/4/1 4:26 下午
 */
@Configuration
//@ConditionalOnMissingBean(ServletInvocableHandlerMethodFactory.class)
public class DefaultServletInvocableHandlerMethodFactory implements ServletInvocableHandlerMethodFactory {
    @Override
    public ServletInvocableHandlerMethod create(HandlerMethod handlerMethod, ServletInvocableHandlerMethodHandler[] handlers) {
        return new IServletInvocableHandlerMethod(handlerMethod, handlers);
    }
}
