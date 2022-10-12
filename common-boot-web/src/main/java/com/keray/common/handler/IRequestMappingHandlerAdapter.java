package com.keray.common.handler;

import com.keray.common.ServletInvocableHandlerMethodFactory;
import com.keray.common.ServletInvocableHandlerMethodHandler;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.ServletInvocableHandlerMethod;

public class IRequestMappingHandlerAdapter extends RequestMappingHandlerAdapter {

    private final ServletInvocableHandlerMethodFactory servletInvocableHandlerMethodFactory;
    private final ServletInvocableHandlerMethodHandler[] handlers;

    public IRequestMappingHandlerAdapter(ServletInvocableHandlerMethodFactory servletInvocableHandlerMethodFactory, ServletInvocableHandlerMethodHandler[] handlers) {
        this.servletInvocableHandlerMethodFactory = servletInvocableHandlerMethodFactory;
        this.handlers = handlers;
    }

    @Override
    public int getOrder() {
        return super.getOrder() - 1;
    }

    @Override
    protected ServletInvocableHandlerMethod createInvocableHandlerMethod(HandlerMethod handlerMethod) {
        return servletInvocableHandlerMethodFactory.create(handlerMethod, handlers);
    }



}
