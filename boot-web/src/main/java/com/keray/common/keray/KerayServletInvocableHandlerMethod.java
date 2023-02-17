package com.keray.common.keray;

import com.keray.common.CommonResultCode;
import com.keray.common.Result;
import com.keray.common.handler.ServletInvocableHandlerMethodCallback;
import com.keray.common.handler.ServletInvocableHandlerMethodHandler;
import com.keray.common.resolver.KerayHandlerMethodArgumentResolverConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.support.HandlerMethodReturnValueHandlerComposite;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.mvc.method.annotation.ServletInvocableHandlerMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 每一个请求都会独立生成一个实例
 *
 * @author by keray
 * date:2020/4/19 1:01 上午
 */
@Slf4j(topic = "api-error")
public class KerayServletInvocableHandlerMethod extends ServletInvocableHandlerMethod {

    private final ServletInvocableHandlerMethodHandler[] handlers;


    private static final Object[] EMPTY_ARGS = new Object[0];


    private final KerayHandlerMethodArgumentResolverConfig resolvers;

    private ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    private HandlerMethodReturnValueHandlerComposite returnValueHandlers;

    private WebDataBinderFactory dataBinderFactory;

    /**
     * 保存当前请求的ServletWebRequest
     */
    private ServletWebRequest webRequest;

    /**
     * 保存当前请求的ModelAndViewContainer
     */
    private ModelAndViewContainer mavContainer;


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

    public void setHandlerMethodReturnValueHandlers(HandlerMethodReturnValueHandlerComposite returnValueHandlers) {
        this.returnValueHandlers = returnValueHandlers;
    }

    /**
     * 照搬源码
     */
    public void invokeAndHandle(ServletWebRequest webRequest, ModelAndViewContainer mavContainer, Object... providedArgs) throws Exception {
        this.webRequest = webRequest;
        this.mavContainer = mavContainer;
        Object returnValue = invokeForRequest(webRequest, mavContainer, providedArgs);
        // 如果是接口已经超时处理后
        if (returnValue instanceof Result<?> r && r.getCode() == CommonResultCode.timeoutOk.getCode()) {
            mavContainer.setRequestHandled(true);
            return;
        }
        breakpointReturn(returnValue);
    }

    public void breakpointReturn(Object returnValue) throws Exception {

        setResponseStatus(webRequest);

        if (returnValue == null) {
            if (isRequestNotModified(webRequest) || getResponseStatus() != null || mavContainer.isRequestHandled()) {
                disableContentCachingIfNecessary(webRequest);
                mavContainer.setRequestHandled(true);
                return;
            }
        } else if (StringUtils.hasText(getResponseStatusReason())) {
            mavContainer.setRequestHandled(true);
            return;
        }

        mavContainer.setRequestHandled(false);
        Assert.state(this.returnValueHandlers != null, "No return value handlers");
        try {
            this.returnValueHandlers.handleReturnValue(
                    returnValue, getReturnValueType(returnValue), mavContainer, webRequest);
        } catch (Exception ex) {
            if (logger.isTraceEnabled()) {
                logger.trace(formatErrorForReturnValue(returnValue), ex);
            }
            throw ex;
        }
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


    private void setResponseStatus(ServletWebRequest webRequest) throws IOException {
        HttpStatus status = getResponseStatus();
        if (status == null) {
            return;
        }

        HttpServletResponse response = webRequest.getResponse();
        if (response != null) {
            String reason = getResponseStatusReason();
            if (StringUtils.hasText(reason)) {
                response.sendError(status.value(), reason);
            } else {
                response.setStatus(status.value());
            }
        }

        // To be picked up by RedirectView
        webRequest.getRequest().setAttribute(View.RESPONSE_STATUS_ATTRIBUTE, status);
    }

    private boolean isRequestNotModified(ServletWebRequest webRequest) {
        return webRequest.isNotModified();
    }

    private void disableContentCachingIfNecessary(ServletWebRequest webRequest) {
        if (isRequestNotModified(webRequest)) {
            HttpServletResponse response = webRequest.getNativeResponse(HttpServletResponse.class);
            Assert.notNull(response, "Expected HttpServletResponse");
            if (StringUtils.hasText(response.getHeader(HttpHeaders.ETAG))) {
                HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
                Assert.notNull(request, "Expected HttpServletRequest");
            }
        }
    }

    private String formatErrorForReturnValue(@Nullable Object returnValue) {
        return "Error handling return value=[" + returnValue + "]" +
                (returnValue != null ? ", type=" + returnValue.getClass().getName() : "") +
                " in " + toString();
    }
}
