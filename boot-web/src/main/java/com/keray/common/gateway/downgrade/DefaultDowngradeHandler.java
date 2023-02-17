package com.keray.common.gateway.downgrade;

import com.alibaba.fastjson2.JSON;
import com.keray.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.HandlerMethod;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 通用接口降级处理器
 */
@Component
@Slf4j
public class DefaultDowngradeHandler implements DefDowngradeHandler {

    @Override
    public Object handler(ApiDowngrade annotation, Result result, NativeWebRequest request, Object[] args, HandlerMethod handlerMethod) {
        var json = annotation.json();
        if (!json.isEmpty()) {
            // 如果json是{} []这种才json.parse 否则直接返回
            if (json.startsWith("{") && json.endsWith("}") ||
                    json.startsWith("[") && json.endsWith("]")) {
                try {
                    return JSON.parse(json);
                } catch (Exception e) {
                    log.error("接口降级json处理失败", e);
                }
            }
            return json;
        }
        var returnType = handlerMethod.getReturnType().getParameterType();
        // 数组默认返回0
        if (Number.class.isAssignableFrom(returnType)) return 0;
        // 字符串类型默认返回空字符串
        if (Character.class.isAssignableFrom(returnType)) return "";
        // list类型默认返回空数组
        if (Collection.class.isAssignableFrom(returnType)) return List.of();
        // map类型返回空map
        if (Map.class.isAssignableFrom(returnType)) return new LinkedHashMap<>();
        // 其他类型返回空实例
        try {
            return returnType.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            log.error("接口降级默认实例化失败" + returnType.getName());
            // 异常返回null
            return result;
        }
    }
}
