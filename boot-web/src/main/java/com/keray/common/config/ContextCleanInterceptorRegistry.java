package com.keray.common.config;

import com.keray.common.context.ThreadCacheContext;
import com.keray.common.mysql.config.MybatisPlusContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Slf4j
@Configuration
public class ContextCleanInterceptorRegistry implements HandlerInterceptor {


    @Resource
    private ApplicationContext applicationContext;

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        var beans = applicationContext.getBeansOfType(ThreadCacheContext.class);
        for (var bean : beans.values()) {
            try {
                bean.clear();
            } catch (Exception e) {
                log.error("清除threadLocalContext失败", e);
            }
        }
        MybatisPlusContext.remove();
    }
}
