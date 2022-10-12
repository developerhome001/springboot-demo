package com.keray.common;

import org.springframework.core.MethodParameter;
import org.springframework.core.Ordered;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;

public interface KerayHandlerMethodArgumentResolver extends HandlerMethodArgumentResolver, Ordered {
    @Deprecated
    default boolean supportsParameter(MethodParameter parameter) {
        return false;
    }

    boolean supportsParameter(MethodParameter parameter, NativeWebRequest webRequest);
}
