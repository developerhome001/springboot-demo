package com.keray.common.gateway.records;

import com.keray.common.handler.ServletInvocableHandlerMethodCallback;
import com.keray.common.handler.ServletInvocableHandlerPipeline;
import com.keray.common.threadpool.SysThreadPool;
import com.keray.common.util.HttpContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.HandlerMethod;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * api接口记录
 */

@Slf4j
@Configuration
@ConditionalOnBean(GatewayRecords.class)
public class RecordsServletInvocableHandlerPipeline implements ServletInvocableHandlerPipeline {

    private final GatewayRecords gatewayRecords;

    public RecordsServletInvocableHandlerPipeline(GatewayRecords gatewayRecords) {
        log.info("添加接口记录器");
        this.gatewayRecords = gatewayRecords;
    }

    @Override
    public int getOrder() {
        return 200;
    }

    @Override
    public Object work(HandlerMethod handlerMethod, Object[] args, NativeWebRequest request, Map<Object, Object> workContext, ServletInvocableHandlerMethodCallback callback) throws Exception {
        HttpServletRequest req = request.getNativeRequest(HttpServletRequest.class);
        RecordsContext context = new RecordsContext();
        int status = gatewayRecords.support(handlerMethod, req, context);
        if (status == 0 || req == null) return callback.get();
        long startTime = System.currentTimeMillis();
        Object result = callback.get();
        long endTime = System.currentTimeMillis();
        var recordsAni = handlerMethod.getMethodAnnotation(ApiRecords.class);
        try {
            // 生成记录上下文
            context.setArgs(args);
            context.setResult(result);
            context.setUri(recordsAni == null ? req.getRequestURI() : recordsAni.value());
            context.setStartTime(startTime);
            context.setEndTime(endTime);
            context.setHeader(header(req));
            context.setCookie(cookie(req));
            context.setIp(HttpContextUtil.getIp(req));
            if (status == 1) {
                gatewayRecords.records(context);
            } else {
                // 异步执行记录函数
                SysThreadPool.execute(() -> gatewayRecords.records(context));
            }
        } catch (Exception e) {
            log.error("日志记录失败", e);

        }
        return result;
    }

    private Map<String, String> header(HttpServletRequest request) {
        Map<String, String> map = new LinkedHashMap<>();
        Enumeration<String> keys = request.getHeaderNames();
        for (String key = keys.nextElement(); keys.hasMoreElements(); key = keys.nextElement()) {
            map.put(key, request.getHeader(key));
        }
        return map;
    }

    private Map<String, List<String>> cookie(HttpServletRequest request) {
        if (request.getCookies() == null) return new HashMap<>();
        Map<String, List<String>> map = new LinkedHashMap<>();
        for (Cookie cookie : request.getCookies()) {
            List<String> old = map.computeIfAbsent(cookie.getName(), k -> new LinkedList<>());
            old.add(cookie.getValue());
        }
        return map;
    }
}
