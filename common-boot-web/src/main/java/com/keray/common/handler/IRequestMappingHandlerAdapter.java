package com.keray.common.handler;

import com.keray.common.ServletInvocableHandlerMethodFactory;
import com.keray.common.ServletInvocableHandlerMethodHandler;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.ServletInvocableHandlerMethod;

import java.util.ArrayList;

public class IRequestMappingHandlerAdapter extends RequestMappingHandlerAdapter {

    private final ServletInvocableHandlerMethodFactory servletInvocableHandlerMethodFactory;

    private final ArrayList<ServletInvocableHandlerMethodHandler> handlers;

    private volatile ServletInvocableHandlerMethodHandler[] cache = null;

    public IRequestMappingHandlerAdapter(ServletInvocableHandlerMethodFactory servletInvocableHandlerMethodFactory, ArrayList<ServletInvocableHandlerMethodHandler> handlers) {
        this.servletInvocableHandlerMethodFactory = servletInvocableHandlerMethodFactory;
        this.handlers = handlers;
    }

    @Override
    public int getOrder() {
        return super.getOrder() - 1;
    }

    @Override
    protected ServletInvocableHandlerMethod createInvocableHandlerMethod(HandlerMethod handlerMethod) {
        if (cache == null) {
            cache = handlers.toArray(new ServletInvocableHandlerMethodHandler[0]);
        }
        return servletInvocableHandlerMethodFactory.create(handlerMethod, cache);
    }



}
