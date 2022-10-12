package com.keray.common.handler;

import com.keray.common.ArgCheckHandler;
import com.keray.common.KerayHandlerMethodArgumentResolver;
import com.keray.common.exception.BizRuntimeException;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodArgumentResolverComposite;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class KerayHandlerMethodArgumentResolverConfig implements HandlerMethodArgumentResolver, ApplicationContextAware {


    private final RequestMappingHandlerAdapter adapter;


    private final HandlerMethodArgumentResolverComposite resolverComposite;


    private final List<KerayHandlerMethodArgumentResolver> kerayArgumentResolvers = new LinkedList<>();

    private Collection<ArgCheckHandler<Object>> argCheckHandlers = null;

    private ApplicationContext applicationContext;

    public KerayHandlerMethodArgumentResolverConfig(RequestMappingHandlerAdapter adapter, HandlerMethodArgumentResolverComposite resolverComposite) {
        this.adapter = adapter;
        this.resolverComposite = resolverComposite;
    }


    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return true;
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        for (var resolver : kerayArgumentResolvers) {
            if (resolver.supportsParameter(parameter, webRequest)) {
                return argCheck(parameter, resolver.resolveArgument(parameter, mavContainer, webRequest, binderFactory));
            }
        }
        return argCheck(parameter, resolverComposite.resolveArgument(parameter, mavContainer, webRequest, binderFactory));
    }

    private Object argCheck(MethodParameter parameter, Object arg) {
        if (argCheckHandlers == null) {
            Map<String, ArgCheckHandler> map = applicationContext.getBeansOfType(ArgCheckHandler.class);
            LinkedList<ArgCheckHandler<Object>> handlers = new LinkedList<>();
            for (ArgCheckHandler handler : map.values()) {
                handlers.add(handler);
            }
            argCheckHandlers = handlers;
        }
        for (var handler : argCheckHandlers) {
            if (handler.support(parameter, arg) && !handler.check(parameter, arg)) {
                throw new BizRuntimeException("请检查输入");
            }
        }
        return arg;
    }

    public void addKerayResolver(KerayHandlerMethodArgumentResolver bean) {
        KerayHandlerMethodArgumentResolver first = kerayArgumentResolvers.isEmpty() ? null : kerayArgumentResolvers.get(0);
        if (first == null) {
            kerayArgumentResolvers.add(bean);
            return;
        }
        if (bean.getOrder() < first.getOrder()) {
            kerayArgumentResolvers.add(0, bean);
            return;
        }
        KerayHandlerMethodArgumentResolver last = kerayArgumentResolvers.get(kerayArgumentResolvers.size() - 1);
        if (bean.getOrder() > last.getOrder()) {
            kerayArgumentResolvers.add(bean);
            return;
        }
        for (var i = 0; i < kerayArgumentResolvers.size() - 1; i++) {
            var item = kerayArgumentResolvers.get(i);
            var item1 = kerayArgumentResolvers.get(i + 1);
            if (item.getOrder() < bean.getOrder() && item1.getOrder() > bean.getOrder()) {
                kerayArgumentResolvers.add(i + 1, bean);
                return;
            }
        }
    }

    public RequestMappingHandlerAdapter getAdapter() {
        return adapter;
    }

    public HandlerMethodArgumentResolverComposite getResolverComposite() {
        return resolverComposite;
    }

    public List<KerayHandlerMethodArgumentResolver> getKerayArgumentResolvers() {
        return kerayArgumentResolvers;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
