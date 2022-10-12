package com.keray.common.handler;

import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.keray.common.KerayHandlerMethodArgumentResolver;
import com.keray.common.annotation.ApiJsonParam;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolverComposite;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.mvc.method.annotation.RequestResponseBodyMethodProcessor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author by keray
 * date:2019/9/2 18:05
 * api端json参数解析
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "keray.api.json.open", havingValue = "true")
@ConfigurationProperties(value = "keray.api.json", ignoreInvalidFields = true)
public class ApiJsonParamResolver extends RequestResponseBodyMethodProcessor implements HandlerInterceptor, KerayHandlerMethodArgumentResolver {

    /**
     * request缓存
     */
    private final ThreadLocal<ServletWebRequest> cache = new ThreadLocal<>();

    private final HandlerMethodArgumentResolverComposite resolverComposite;

    /**
     * 全局开关 全局开发打开意味着改装载器会处理所有application/json请求的方法，不开启全局开关时仅当方法具有@ApiJsonParam(value = true)时有效
     */
    @Setter
    @Getter
    private Boolean globalSwitch = true;

    private static final String ROOT_JSON_KEY = "root-json-key";


    public ApiJsonParamResolver(KerayHandlerMethodArgumentResolverConfig kerayHandlerMethodArgumentResolverConfig) {
        super(kerayHandlerMethodArgumentResolverConfig.getAdapter().getMessageConverters());
        this.resolverComposite = kerayHandlerMethodArgumentResolverConfig.getResolverComposite();
        kerayHandlerMethodArgumentResolverConfig.addKerayResolver(this);
    }


    @Override
    public boolean supportsParameter(MethodParameter parameter, NativeWebRequest webRequest) {
        HttpServletRequest httpServletRequest = webRequest.getNativeRequest(HttpServletRequest.class);
        if (httpServletRequest == null || "GET".equals(httpServletRequest.getMethod()) ||
                httpServletRequest.getContentType() == null ||
                !httpServletRequest.getContentType().contains(MediaType.APPLICATION_JSON_VALUE))
            return false;
        ApiJsonParam apiJsonParam = parameter.getMethodAnnotation(ApiJsonParam.class);
        // 对于RequestBody注解的方法不做处理
        if (globalSwitch) {
            if (apiJsonParam != null) {
                return apiJsonParam.value() && parameter.getParameterAnnotation(RequestBody.class) == null;
            }
            return parameter.getParameterAnnotation(RequestBody.class) == null;
        } else {
            return apiJsonParam != null && apiJsonParam.value() && parameter.getParameterAnnotation(RequestBody.class) == null;
        }
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        HttpServletRequest httpServletRequest = webRequest.getNativeRequest(HttpServletRequest.class);
        // 仅当content-type为app/json时 处理 其他情况走原有的处理
        all:
        if (cache.get() == null) {
            MethodParameter mapParam = new MethodParameter(parameter) {
                // 重写为了将body的json字符串转换为map对象
                @Override
                public Class<?> getParameterType() {
                    return Map.class;
                }
            };
            Map<String, Object> data = (Map<String, Object>) readWithMessageConverters(webRequest, mapParam, mapParam.getParameterType());
            if (data == null) {
                log.warn("解析body为空json");
                break all;
            }
            Map<String, String[]> paramMap = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                if (entry.getValue() == null) {
                    paramMap.put(entry.getKey(), null);
                    continue;
                }
                // 处理基本数据类型
                if (ClassUtil.isSimpleValueType(entry.getValue().getClass())) {
                    paramMap.put(entry.getKey(), new String[]{entry.getValue().toString()});
                }
                // 处理数组类型
                else if (entry.getValue() instanceof List) {
                    paramMap.put(entry.getKey(), ((List<Object>) entry.getValue())
                            .stream()
                            .map(s -> {
                                if (s == null) {
                                    return null;
                                }
                                if (ClassUtil.isSimpleValueType(s.getClass())) {
                                    return s.toString();
                                }
                                return JSON.toJSONString(s);
                            }).toArray(String[]::new));
                }
                // 处理对象类型 数组类型
                else if (entry.getValue() instanceof Map) {
                    // 将map转换为json放到[0]
                    paramMap.put(entry.getKey(), new String[]{JSON.toJSONString(entry.getValue())});
                } else {
                    log.warn("无法解析复杂类型 value={},class={}", entry.getValue(), entry.getValue().getClass());
                }
            }
            // 保留一份原始的json
            paramMap.put(ROOT_JSON_KEY, new String[]{JSON.toJSONString(data)});
            ServletWebRequest servletWebRequest = new ServletWebRequest(new IHttpServletRequest(httpServletRequest),
                    (HttpServletResponse) webRequest.getNativeResponse());
            servletWebRequest.getParameterMap().putAll(paramMap);
            cache.set(servletWebRequest);
        }
        webRequest = cache.get();
        // 处理对象类型
        ModelAttribute attribute = parameter.getParameterAnnotation(ModelAttribute.class);
        if (attribute != null) {
            String attributeJson;
            // 一级ModelAttribute时直接将整个json放入body
            if (StrUtil.isBlank(attribute.value())) {
                String[] data = webRequest.getParameterMap().get(ROOT_JSON_KEY);
                attributeJson = data == null ? null : data[0];
            }
            // 二级ModelAttribute（最多只能出现两级），在上面放到ParameterMap的json字符串拿出
            else {
                //
                if (Collection.class.isAssignableFrom(parameter.getParameterType())) {
                    String[] value = webRequest.getParameterValues(attribute.value());
                    if (value == null) {
                        attributeJson = "[]";
                    } else {
                        attributeJson = StrUtil.format("[{}]",
                                String.join(",", value));
                    }
                } else {
                    attributeJson = webRequest.getParameter(attribute.value());
                }
            }
            // 拿到自定义的HttpServletRequest
            IHttpServletRequest iHttpServletRequest = (IHttpServletRequest) webRequest.getNativeRequest();
            // 将json字符串写入自定义的HttpServletRequest的body里
            iHttpServletRequest.setBody(attributeJson == null ? new byte[0] : attributeJson.getBytes());
            // 最后直接使用标准的RequestBody装载器装载参数
            // 使用RequestBody装载器装载可以使得复杂model也能完成装载
            return super.resolveArgument(parameter, mavContainer, webRequest, binderFactory);
        }

        return resolverComposite.resolveArgument(parameter, mavContainer, webRequest, binderFactory);
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        cache.remove();
    }


    @Override
    public int getOrder() {
        return 100;
    }
}


