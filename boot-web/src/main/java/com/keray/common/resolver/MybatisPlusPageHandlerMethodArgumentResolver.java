package com.keray.common.resolver;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.map.MapUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.core.MethodParameter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.mvc.method.annotation.RequestResponseBodyMethodProcessor;
import org.springframework.web.servlet.mvc.method.annotation.ServletModelAttributeMethodProcessor;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author by keray
 * date:2021/7/9 9:58 上午
 */
public class MybatisPlusPageHandlerMethodArgumentResolver implements HandlerMethodArgumentResolver {

    private final ServletModelAttributeMethodProcessor servletModelAttributeMethodProcessor = new ServletModelAttributeMethodProcessor(true);

    private final RequestResponseBodyMethodProcessor requestResponseBodyMethodProcessor;

    public MybatisPlusPageHandlerMethodArgumentResolver(List<HttpMessageConverter<?>> converters) {
        requestResponseBodyMethodProcessor = new RequestResponseBodyMethodProcessor(converters);
    }

    @Override
    public boolean supportsParameter(MethodParameter methodParameter) {
        return methodParameter.getParameterAnnotation(ModelAttribute.class) != null && methodParameter.getParameterType().equals(Page.class);
    }

    @Override
    public Object resolveArgument(MethodParameter methodParameter, ModelAndViewContainer modelAndViewContainer, NativeWebRequest nativeWebRequest, WebDataBinderFactory webDataBinderFactory) throws Exception {
        HttpServletRequest httpServletRequest = nativeWebRequest.getNativeRequest(HttpServletRequest.class);
        if ("GET".equals(httpServletRequest.getMethod())) {
            if (httpServletRequest.getParameterMap().containsKey("orders[column]")) {
                String[] columns = httpServletRequest.getParameterValues("orders[column]");
                String[] asc = httpServletRequest.getParameterValues("orders[asc]");
                Map<String, String[]> data = httpServletRequest.getParameterMap();
                Map<String, Object> copy = new HashMap<>();
                for (Map.Entry<String, String[]> entry : data.entrySet()) {
                    String key = entry.getKey();
                    if (entry.getValue().length == 1) {
                        copy.put(key, entry.getValue()[0]);
                    }
                }
                List<Object> orders = new ArrayList<>();
                for (int i = 0; i < columns.length; i++) {
                    orders.add(i, MapUtil.builder()
                            .put("column", columns[i])
                            .put("asc", i < asc.length ? asc[i] : true)
                            .build());
                }
                copy.put("orders", orders);
                return BeanUtil.toBean(copy, Page.class);
            } else {
                return servletModelAttributeMethodProcessor.resolveArgument(methodParameter, modelAndViewContainer, nativeWebRequest, webDataBinderFactory);
            }
        }
        return requestResponseBodyMethodProcessor.resolveArgument(methodParameter, modelAndViewContainer, nativeWebRequest, webDataBinderFactory);
    }
}
