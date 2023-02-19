package com.keray.common.handler;

import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.HandlerMethod;

import java.util.Map;

/**
 * 管道管理器
 */
public interface ServletInvocableHandlerPipelineChina {

    /**
     * 现在所处管道的位置
     *
     * @return
     */
    int nowPipelineIndex();

    Object work(HandlerMethod handlerMethod, Object[] args, NativeWebRequest request, Map<Object, Object> workContext) throws Exception;
}