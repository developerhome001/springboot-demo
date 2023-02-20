package com.keray.common.handler;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.keray.common.IUserContext;
import com.keray.common.Result;
import com.keray.common.SpringContextHolder;
import com.keray.common.SystemProperty;
import com.keray.common.annotation.ApiLogIgnore;
import com.keray.common.entity.IBaseEntity;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.HandlerMethod;

import javax.annotation.Resource;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author by keray
 * date:2020/6/3 10:08 上午
 */
@Slf4j
@Configuration
@ConfigurationProperties(prefix = "keray.api.log")
public class ApiLogServletInvocableHandlerPipeline implements ServletInvocableHandlerPipeline {

    @Resource(name = "kObjectMapper")
    private ObjectMapper objectMapper;

    @Resource
    private IUserContext userContext;

    @Getter
    @Setter
    private Boolean all = false;

    @Override
    public int getOrder() {
        return 100;
    }

    @Override
    public Object work(HandlerMethod handlerMethod, Object[] args, NativeWebRequest request, Map<Object, Object> workContext, ServletInvocableHandlerMethodCallback callback) throws Exception {
        long start = System.currentTimeMillis();
        for (Object o : args) {
            if (o instanceof IBaseEntity) {
                ((IBaseEntity) o).clearBaseField();
            }
        }
        Consumer<Object> logFail = result -> {
            try {
                var data = handlerMethod.getMethodAnnotation(ApiLogIgnore.class);
                if (result instanceof Result.FailResult || result instanceof Exception || (data == null && all) || data != null && !data.value()) {
                    String url = null;
                    String flag = null;
                    try {
                        HttpServletRequest servletRequest = request.getNativeRequest(HttpServletRequest.class);
                        if (servletRequest != null) {
                            url = servletRequest.getRequestURL().toString();
                            if (StrUtil.isBlank(url)) {
                                url = "错误";
                            }
                            flag = servletRequest.getHeader("X-User-Agent");
                            if (StrUtil.isBlank(flag)) {
                                flag = servletRequest.getHeader("User-Agent");
                            }
                            if (StrUtil.isBlank(flag)) {
                                flag = "未知";
                            }
                        }
                    } catch (Exception e) {
                        log.error("api解析标志失败：", e);
                    }
                    apiLog(result instanceof Result.FailResult || result instanceof Exception,
                            result, url, flag, args, handlerMethod.getMethodParameters(),
                            start, handlerMethod, request);
                }
            } catch (Exception e) {
                log.error("api错误日志解析失败：", e);
            }
        };
        try {
            Object result = callback.get();
            logFail.accept(result);
            return result;
        } catch (Throwable e) {
            logFail.accept(e);
            throw e;
        }
    }

    private void apiLog(boolean fail,
                        Object result,
                        String url,
                        String flag,
                        Object[] args,
                        MethodParameter[] parameters,
                        long start,
                        HandlerMethod handlerMethod,
                        NativeWebRequest request
    ) throws JsonProcessingException {
        StringBuilder builder = new StringBuilder();
        if (fail) {
            builder.append(System.lineSeparator()).append("============接口异常============").append(System.lineSeparator());
        } else {
            builder.append(System.lineSeparator()).append("============api start============").append(System.lineSeparator());
        }
        var appid = "";
        var appidHeader = System.getProperty(SystemProperty.HEADER_APPID);
        if (appidHeader != null) {
            appid = request.getHeader(appidHeader);
        }
        builder.append("  flag:").append(flag).append(System.lineSeparator());
        builder.append("   url:").append(url).append(System.lineSeparator());
        builder.append("    ip:").append(userContext.currentIp()).append(System.lineSeparator());
        builder.append("   uid:").append(userContext.currentUserId()).append(System.lineSeparator());
        builder.append("  duid:").append(userContext.getDuid()).append(System.lineSeparator());
        builder.append(" appid:").append(appid).append(System.lineSeparator());
        builder.append("  args:").append(System.lineSeparator());
        for (int i = 0; i < parameters.length; i++) {
            String json = "json解析失败";
            var item = args[i];
            try {
                if (item instanceof ServletResponse) {
                    json = item.toString();
                } else {
                    json = args[i] == null ? null : objectMapper.writeValueAsString(item);
                }
            } catch (Exception ignore) {
            }
            builder.append(parameters[i].getParameterName()).append("=").append(json).append(System.lineSeparator());
        }
        if (result instanceof Result.FailResult) {
            builder.append("result:").append(StrUtil.format("接口降级={},code={},message={}", ((Result.FailResult<?, ?>) result).isApiDown(), ((Result) result).getCode(), ((Result.FailResult) result).getMessage())).append(System.lineSeparator());
        } else if (result instanceof Result.SuccessResult) {
            builder.append("result:").append(objectMapper.writeValueAsString(((Result.SuccessResult) result).getData())).append(System.lineSeparator());
        } else if (result == null) {
            builder.append("result:NULL").append(System.lineSeparator());
        } else {
            String re = result.toString();
            builder.append("result:").append(re, 0, Math.min(4048, re.length())).append(re.length() >= 4048 ? "···" : "").append(" =>").append(result.getClass()).append(System.lineSeparator());
        }
        builder.append(line(handlerMethod)).append(System.lineSeparator());
        builder.append(String.format("============end time=%sms  ============", System.currentTimeMillis() - start));
        builder.append(System.lineSeparator());
        if (fail) {
            if (result instanceof Result.FailResult) {
                log.error(builder.toString(), ((Result.FailResult<?, ?>) result).getError());
            } else {
                log.error(builder.toString(), result);
            }
        } else {
            log.info(builder.toString());
        }
    }

    private String line(HandlerMethod handlerMethod) {
        String active = SpringContextHolder.getEnvironmentProperty("spring.profiles.active");
        if ("dev".equals(active)) {
            try {
                ClassPool pool = ClassPool.getDefault();
                Method method = handlerMethod.getMethod();
                CtClass cc = pool.get(method.getDeclaringClass().getName());
                Class[] types = method.getParameterTypes();
                CtClass[] ctClasses = new CtClass[types.length];
                for (int i = 0; i < types.length; i++) {
                    ctClasses[i] = pool.get(types[i].getName());
                }
                CtMethod methodX = cc.getDeclaredMethod(method.getName(), ctClasses);
                return StrUtil.format("at:{}.{}({}.java:{})", method.getDeclaringClass().getName(), method.getName(), method.getDeclaringClass().getSimpleName(), methodX.getMethodInfo().getLineNumber(0));
            } catch (Exception ignore) {
                ignore.printStackTrace();
            }
        }
        return "";
    }
}
