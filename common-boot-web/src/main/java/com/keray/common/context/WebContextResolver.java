package com.keray.common.context;

import cn.hutool.core.util.StrUtil;
import com.keray.common.KerayHandlerMethodArgumentResolver;
import com.keray.common.handler.KerayHandlerMethodArgumentResolverConfig;
import com.keray.common.util.HttpWebUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * token参数解析
 */
@Configuration
public class WebContextResolver implements KerayHandlerMethodArgumentResolver {

    public WebContextResolver(KerayHandlerMethodArgumentResolverConfig kerayHandlerMethodArgumentResolverConfig) {
        kerayHandlerMethodArgumentResolverConfig.addKerayResolver(this);
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter, NativeWebRequest webRequest, Map<Object, Object> threadLocal) {
        return parameter.getParameterType() == WebContext.class;
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory, Map<Object, Object> threadLocal) throws Exception {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        if (request == null) return new WebContext();
        return WebContext.builder()
                .deviceType(HttpWebUtil.currentDeviceType(request))
                .uuid(HttpWebUtil.duuid(request))
                .host(HttpWebUtil.host(request))
                .ip(getIp(request))
                .build();
    }


    @Override
    public int getOrder() {
        return 5;
    }

    public String getIp(HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        String forwardedIp = request.getHeader("x-forwarded-for");
        if (StrUtil.isNotBlank(forwardedIp)) {
            String[] ips = forwardedIp.split(",");
            return ips[0];
        }
        return ip;
    }
}
