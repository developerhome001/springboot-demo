package com.keray.common.resolver;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.converter.BufferedImageHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import org.springframework.web.method.support.HandlerMethodArgumentResolverComposite;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.LinkedList;

/**
 * @author by keray
 * date:2020/1/10 3:33 PM
 */
@Slf4j
@Configuration
public class ResolverConfig {

    @Resource
    private RequestMappingHandlerAdapter adapter;

    /**
     * <p>
     * <h3>作者 keray</h3>
     * <h3>时间： 2019/9/3 14:50</h3>
     * 添加自定义ApiJsonParam装载器
     * </p>
     *
     * @param
     * @return <p> {@link } </p>
     * @throws
     */
    @Bean
    public KerayHandlerMethodArgumentResolverConfig kerayHandlerMethodArgumentResolver(@Qualifier("kObjectMapper") ObjectMapper objectMapper) {
        configBug(objectMapper);
        var springResolvers = adapter.getArgumentResolvers();
        var resolverComposite = new HandlerMethodArgumentResolverComposite();
        resolverComposite.addResolver(new PageableHandlerMethodArgumentResolver());
        try {
            Class.forName("com.baomidou.mybatisplus.extension.plugins.pagination.Page");
            resolverComposite.addResolver(new MybatisPlusPageHandlerMethodArgumentResolver(adapter.getMessageConverters()));
        }catch (ClassNotFoundException e) {
            log.warn("mybatis plus不存在");
        }
        resolverComposite.addResolvers(springResolvers);
        var root = new KerayHandlerMethodArgumentResolverConfig(adapter, resolverComposite);
        adapter.setArgumentResolvers(new LinkedList<>() {{
            add(root);
        }});
        return root;
    }

    public void configBug(ObjectMapper objectMapper) {
        // （原有：项目原先混乱的结构导致直接注入的支持LocalDatetime的JavaTimeModule被不支持的覆盖掉，这里就直接替换了MappingJackson2HttpMessageConverter）
        // 替换
        for (HttpMessageConverter c : adapter.getMessageConverters()) {
            if (c instanceof MappingJackson2HttpMessageConverter) {
                if (!Collections.replaceAll(adapter.getMessageConverters(), c, new MappingJackson2HttpMessageConverter(objectMapper))) {
                    throw new RuntimeException("添加localDatetime转换器异常");
                }
                break;
            }
        }
        // 交换xml json解析器
        int a = -1, b = -1;
        for (int i = 0; i < adapter.getMessageConverters().size(); i++) {
            if (adapter.getMessageConverters().get(i) instanceof MappingJackson2HttpMessageConverter) {
                a = i;
            }
            if (adapter.getMessageConverters().get(i) instanceof MappingJackson2XmlHttpMessageConverter) {
                b = i;
            }
        }
        if (a != -1 && b != -1 && a > b) {
            HttpMessageConverter<?> mappingJackson2HttpMessageConverter = adapter.getMessageConverters().get(a);
            adapter.getMessageConverters().set(a, adapter.getMessageConverters().get(b));
            adapter.getMessageConverters().set(b, mappingJackson2HttpMessageConverter);
        }
        adapter.getMessageConverters().add(addConverter());
    }

    public BufferedImageHttpMessageConverter addConverter() {
        return new BufferedImageHttpMessageConverter();
    }
}
