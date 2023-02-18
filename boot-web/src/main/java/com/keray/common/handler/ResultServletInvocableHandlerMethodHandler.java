package com.keray.common.handler;

import com.keray.common.Result;
import com.keray.common.annotation.ApiResult;
import com.keray.common.annotation.ResultIgnore;
import com.keray.common.utils.CommonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.HandlerMethod;

import java.util.Map;

/**
 * @author by keray
 * date:2020/9/7 9:36 下午
 */
@Slf4j
@Configuration
public class ResultServletInvocableHandlerMethodHandler implements ServletInvocableHandlerMethodHandler {

    @Override
    public int getOrder() {
        return 1000;
    }

    @Override
    public Object work(HandlerMethod handlerMethod, Object[] args, NativeWebRequest request, Map<Object, Object> workContext, ServletInvocableHandlerMethodCallback callback) throws Exception {
        Object result = callback.get();
        ApiResult apiResult = handlerMethod.getMethodAnnotation(ApiResult.class);
        if (apiResult == null) {
            apiResult = CommonUtil.getClassAllAnnotation(handlerMethod.getMethod().getDeclaringClass(), ApiResult.class);
        }
        if (result instanceof Result || apiResult == null ||
                handlerMethod.getMethodAnnotation(ResultIgnore.class) != null ||
                CommonUtil.getClassAllAnnotation(handlerMethod.getMethod().getDeclaringClass(), ResultIgnore.class) != null) {
            return result;
        }
        return Result.success(result);
    }
}
