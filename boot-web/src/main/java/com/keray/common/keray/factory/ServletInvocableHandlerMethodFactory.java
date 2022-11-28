package com.keray.common.keray.factory;

import com.keray.common.handler.ServletInvocableHandlerMethodHandler;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.annotation.ServletInvocableHandlerMethod;

/**
 * @author by keray
 * date:2021/4/1 4:22 下午
 * 自定义创建 {@link ServletInvocableHandlerMethod}
 * 默认实现 {@link DefaultServletInvocableHandlerMethodFactory}
 */
public interface ServletInvocableHandlerMethodFactory {
    ServletInvocableHandlerMethod create(HandlerMethod handlerMethod, ServletInvocableHandlerMethodHandler[] handlers);
}
