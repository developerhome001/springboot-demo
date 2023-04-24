package com.keray.common.gateway.downgrade;

import com.keray.common.Result;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.HandlerMethod;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * 接口降级处理器
 */
public interface DowngradeHandler {

    /**
     * @param annotation    降级注解
     * @param result        错误响应  超时时result可能是success
     * @param request       请求
     * @param args          接口参数
     * @param handlerMethod 接口对象
     * @return
     */
    Object handler(ApiDowngrade annotation, Result result, ServletRequest request, ServletResponse response, Object[] args, HandlerMethod handlerMethod);
}
