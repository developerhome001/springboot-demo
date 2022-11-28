package com.keray.common.resolver;

import org.springframework.core.MethodParameter;
import org.springframework.core.Ordered;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.HashMap;
import java.util.Map;

public interface KerayHandlerMethodArgumentResolver extends HandlerMethodArgumentResolver, Ordered {
    @Deprecated
    default boolean supportsParameter(MethodParameter parameter) {
        return false;
    }

    boolean supportsParameter(MethodParameter parameter, NativeWebRequest webRequest, Map<Object, Object> threadLocal);

    @Override
    default Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        return resolveArgument(parameter, mavContainer, webRequest, binderFactory, new HashMap<>());
    }

    Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory, Map<Object, Object> threadLocal) throws Exception;
}
