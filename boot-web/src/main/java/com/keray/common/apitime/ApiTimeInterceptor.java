package com.keray.common.apitime;

import com.keray.common.annotation.ApiTimeRecord;
import com.keray.common.handler.ServletInvocableHandlerMethodCallback;
import com.keray.common.handler.ServletInvocableHandlerMethodHandler;
import com.keray.common.threadpool.SysThreadPool;
import com.keray.common.utils.CommonUtil;
import com.keray.common.utils.TimeUtil;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.HandlerMethod;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

/**
 * @author by keray
 * date:2019/12/4 2:21 PM
 * 记录api接口调用时长
 */
@Slf4j
public class ApiTimeInterceptor implements ServletInvocableHandlerMethodHandler {

    @Resource
    private ApiTimeRecordDb apiTimeRecordDao;

    public ApiTimeInterceptor() {
        log.info("开启接口时间记录");
    }

    @Override
    public int getOrder() {
        return 900;
    }

    @Override
    public Object work(HandlerMethod handlerMethod, Object[] args, NativeWebRequest request, Map<Object, Object> workContext, ServletInvocableHandlerMethodCallback callback) throws Exception {
        TimeData data = preHandle(handlerMethod);
        try {
            return callback.get();
        } finally {
            postHandle(data, request.getNativeRequest(HttpServletRequest.class), handlerMethod);
        }
    }

    public TimeData preHandle(HandlerMethod handler) {
        ApiTimeRecord record = handler.getMethodAnnotation(ApiTimeRecord.class);
        if (record == null) {
            record = CommonUtil.getClassAllAnnotation(handler.getMethod().getDeclaringClass(), ApiTimeRecord.class);
        }
        if (record != null) {
            TimeData data = new TimeData();
            data.start = System.currentTimeMillis();
            // api接口开启了记录
            data.gt = record.gt();
            if ("".equals(record.value())) {
                Operation apiOperation = handler.getMethodAnnotation(Operation.class);
                if (apiOperation != null) {
                    data.title = apiOperation.summary();
                } else {
                    data.title = handler.getMethod().getName();
                }
            } else {
                data.title = record.value();
            }
            return data;
        }
        return null;
    }

    private void postHandle(TimeData data, HttpServletRequest request, HandlerMethod handlerMethod) {
        if (data != null) {
            data.end = System.currentTimeMillis();
            String url;
            try {
                url = request.getRequestURL().toString();
            } catch (Exception ignore) {
                url = "异常";
            }
            String finalUrl = url;
            SysThreadPool.execute(() -> {
                if (data.end - data.start > data.gt) {
                    apiTimeRecordDao.insert(apiTimeRecordDao.build(ApiTimeRecordData.builder()
                            .execTime((int) (data.end - data.start))
                            .gt(data.gt)
                            .title(data.title)
                            .url(finalUrl)
                            .time(TimeUtil.DATE_TIME_FORMATTER_MS.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(data.start), ZoneId.systemDefault())))
                            .methodPath(handlerMethod.getMethod().getDeclaringClass().getName() + "#" + handlerMethod.getMethod().getName())
                            .build()));
                }
            }, false);
        }
    }

    private static class TimeData {
        String title;
        int gt;
        long start;
        long end;

    }
}
