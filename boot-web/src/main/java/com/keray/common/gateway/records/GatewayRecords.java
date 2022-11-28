package com.keray.common.gateway.records;

import org.springframework.web.method.HandlerMethod;

import javax.servlet.http.HttpServletRequest;

public interface GatewayRecords {

    /**
     * 判断这次请求是否需要记录
     *
     * @param method
     * @param request
     * @return 0 不记录 1 同步记录 2异步记录
     */
    int support(HandlerMethod method, HttpServletRequest request);


    /**
     * 接口记录
     *
     * @param context
     */
    void records(RecordsContext context);
}
