package com.keray.common.keray;

import com.keray.common.handler.ServletInvocableHandlerMethodCallback;
import com.keray.common.handler.ServletInvocableHandlerMethodHandler;
import com.keray.common.resolver.KerayHandlerMethodArgumentResolverConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.mvc.method.annotation.ServletInvocableHandlerMethod;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author by keray
 * date:2020/4/19 1:01 上午
 */
@Slf4j(topic = "api-error")
public class KerayServletInvocableHandlerMethod extends ServletInvocableHandlerMethod {

    private final ServletInvocableHandlerMethodHandler[] handlers;


    private static final Object[] EMPTY_ARGS = new Object[0];


    private final KerayHandlerMethodArgumentResolverConfig resolvers;

    private ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    @Nullable
    private WebDataBinderFactory dataBinderFactory;


    public KerayServletInvocableHandlerMethod(HandlerMethod handlerMethod, ServletInvocableHandlerMethodHandler[] handlers, KerayHandlerMethodArgumentResolverConfig resolvers) {
        super(handlerMethod);
        this.handlers = handlers;
        this.resolvers = resolvers;
    }

    public void setDataBinderFactory(WebDataBinderFactory dataBinderFactory) {
        this.dataBinderFactory = dataBinderFactory;
    }


    @Override
    public void setParameterNameDiscoverer(ParameterNameDiscoverer parameterNameDiscoverer) {
        this.parameterNameDiscoverer = parameterNameDiscoverer;
    }

    @Override
    public Object invokeForRequest(NativeWebRequest request, ModelAndViewContainer mavContainer, Object... providedArgs) throws Exception {
        Map<Object, Object> threadLocal = new HashMap<>();
        try {
            Object[] args = getMethodArgumentValues(threadLocal, request, mavContainer, providedArgs);
            return work(args, request, () -> doInvoke(args));
        } finally {
            threadLocal.clear();
        }
    }

    protected Object work(Object[] args, NativeWebRequest request, ServletInvocableHandlerMethodCallback call) throws Exception {
        if (handlers != null) {
            final AtomicInteger index = new AtomicInteger(0);
            AtomicReference<ServletInvocableHandlerMethodCallback> callback1 = new AtomicReference<>(null);
            ServletInvocableHandlerMethodCallback callback = () -> {
                index.getAndIncrement();
                if (index.get() == handlers.length) {
                    return call.get();
                }
                return handlers[index.get()].work(this, args, request, callback1.get());
            };
            callback1.set(callback);
            return handlers[index.get()].work(this, args, request, callback);
        } else {
            return call.get();
        }
    }

    protected Object[] getMethodArgumentValues(Map<Object, Object> threadLocal, NativeWebRequest request, ModelAndViewContainer mavContainer, Object... providedArgs) throws Exception {

        MethodParameter[] parameters = getMethodParameters();
        if (ObjectUtils.isEmpty(parameters)) {
            return EMPTY_ARGS;
        }

        Object[] args = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            MethodParameter parameter = parameters[i];
            parameter.initParameterNameDiscovery(this.parameterNameDiscoverer);
            args[i] = findProvidedArgument(parameter, providedArgs);
            if (args[i] != null) {
                continue;
            }
            try {
                args[i] = this.resolvers.resolveArgument(parameter, mavContainer, request, this.dataBinderFactory, threadLocal);
            } catch (Exception ex) {
                if (logger.isDebugEnabled()) {
                    String exMsg = ex.getMessage();
                    if (exMsg != null && !exMsg.contains(parameter.getExecutable().toGenericString())) {
                        logger.debug(formatArgumentError(parameter, exMsg));
                    }
                }
                throw ex;
            }
        }
        return args;
    }
}
