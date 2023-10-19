package com.keray.common.filter;

import cn.hutool.core.util.StrUtil;
import com.keray.common.IUserContext;
import com.keray.common.SystemProperty;
import com.keray.common.context.ThreadCacheContext;
import com.keray.common.mysql.config.MybatisPlusContext;
import com.keray.common.util.HttpContextUtil;
import com.keray.common.util.HttpWebUtil;
import com.keray.common.utils.UUIDUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import javax.annotation.Resource;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author by keray
 * date:2019/10/24 11:43
 */
@Slf4j
@Configuration
@ConditionalOnBean(value = IUserContext.class)
@Order(Integer.MIN_VALUE)
public class BaseMessageFilter extends KerayFilter {
    // 设备uuid
    public static final String TOKEN_DEVICE_UUID_KEY = "duid";

    @Resource
    private IUserContext<?> userContext;

    @Resource
    private ApplicationContext applicationContext;


    private final DuidGenerate duidGenerate;

    public BaseMessageFilter(ObjectProvider<DuidGenerate> duidGenerate) {
        this.duidGenerate = duidGenerate.getIfAvailable(() -> UUIDUtil::generateUUIDByTimestamp);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest httpServletRequest, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            // 设置request上下文
            userContext.setCurrentRequest(httpServletRequest);
            // 设置当前id
            userContext.setIp(HttpContextUtil.getIp(httpServletRequest));
            var duid = getRequestUUID(httpServletRequest);
            if (StrUtil.isEmpty(duid)) {
                duid = generateBrowserUUID(response);
            }
            userContext.setDUid(duid);
            filterChain.doFilter(httpServletRequest, response);
        } finally {
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

    public String getRequestUUID(HttpServletRequest request) {
        String uid = request.getHeader(BaseMessageFilter.TOKEN_DEVICE_UUID_KEY);
        if (StrUtil.isBlank(uid)) {
            if (request.getCookies() == null) {
                return null;
            }
            uid = HttpWebUtil.getCookieValue(request, BaseMessageFilter.TOKEN_DEVICE_UUID_KEY);
        }
        return uid;
    }

    public String generateBrowserUUID(HttpServletResponse response) {
        String uuid = duidGenerate.generate();
        Cookie cookie = new Cookie(BaseMessageFilter.TOKEN_DEVICE_UUID_KEY, uuid);
        cookie.setPath("/");
        cookie.setMaxAge(3600 * 24 * 9999);
        cookie.setHttpOnly(false);
        cookie.setValue(uuid);
        var domain = System.getProperty(SystemProperty.DUID_DOMAIN, "");
        if (StrUtil.isNotBlank(domain)) cookie.setDomain(domain);
        response.addCookie(cookie);
        return uuid;
    }
}