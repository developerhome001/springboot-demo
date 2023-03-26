package com.keray.common.keray;

import com.keray.common.handler.ServletInvocableHandlerPipeline;
import com.keray.common.keray.factory.ServletInvocableHandlerMethodFactory;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.ServletInvocableHandlerMethod;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class KerayRequestMappingHandlerAdapter extends RequestMappingHandlerAdapter {

    private final ServletInvocableHandlerMethodFactory servletInvocableHandlerMethodFactory;

    private final Set<ServletInvocableHandlerPipeline> handlers;

    private volatile ServletInvocableHandlerPipeline[] cache = null;

    public KerayRequestMappingHandlerAdapter(ServletInvocableHandlerMethodFactory servletInvocableHandlerMethodFactory, Set<ServletInvocableHandlerPipeline> handlers) {
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
            cache = handlers.stream()
                    .sorted(Comparator.comparing(ServletInvocableHandlerPipeline::getOrder))
                    .toArray(ServletInvocableHandlerPipeline[]::new);
        }
        return servletInvocableHandlerMethodFactory.create(handlerMethod, cache);
    }


}
