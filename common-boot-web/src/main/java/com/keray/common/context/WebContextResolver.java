package com.keray.common.context;

import com.keray.common.IUserContext;
import com.keray.common.resolver.KerayHandlerMethodArgumentResolver;
import com.keray.common.resolver.KerayHandlerMethodArgumentResolverConfig;
import com.keray.common.util.HttpContextUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * token参数解析
 */
@Configuration
public class WebContextResolver implements KerayHandlerMethodArgumentResolver {

    @Resource
    private IUserContext<?> userContext;

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
                .deviceType(HttpContextUtil.currentDeviceType(request))
                .uuid(userContext.getDuid())
                .host(HttpContextUtil.host(request))
                .ip(userContext.currentIp())
                .build();
    }


    @Override
    public int getOrder() {
        return 5;
    }

}
