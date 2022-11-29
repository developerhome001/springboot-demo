package com.keray.common.keray;

import com.keray.common.handler.ServletInvocableHandlerMethodHandler;
import com.keray.common.keray.factory.ServletInvocableHandlerMethodFactory;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.ServletInvocableHandlerMethod;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class KerayRequestMappingHandlerAdapter extends RequestMappingHandlerAdapter {

    private final ServletInvocableHandlerMethodFactory servletInvocableHandlerMethodFactory;

    private final List<ServletInvocableHandlerMethodHandler> handlers;

    private volatile ServletInvocableHandlerMethodHandler[] cache = null;

    public KerayRequestMappingHandlerAdapter(ServletInvocableHandlerMethodFactory servletInvocableHandlerMethodFactory, List<ServletInvocableHandlerMethodHandler> handlers) {
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
            handlers.sort(Comparator.comparing(ServletInvocableHandlerMethodHandler::getOrder));
            cache = handlers.toArray(new ServletInvocableHandlerMethodHandler[0]);
        }
        return servletInvocableHandlerMethodFactory.create(handlerMethod, cache);
    }


}
