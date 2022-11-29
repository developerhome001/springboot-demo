package com.keray.common.filter;

import cn.hutool.core.util.StrUtil;
import com.keray.common.IUserContext;
import com.keray.common.util.HttpContextUtil;
import com.keray.common.util.HttpWebUtil;
import com.keray.common.utils.UUIDUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import javax.annotation.Resource;
import javax.servlet.*;
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
public class BaseMessageFilter implements Filter {
    // 设备uuid
    public static final String TOKEN_DEVICE_UUID_KEY = "duid";

    @Resource
    private IUserContext<?> userContext;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (request instanceof HttpServletRequest httpServletRequest) {
            // 设置request上下文
            userContext.setCurrentRequest(httpServletRequest);
            // 设置当前id
            userContext.setIp(HttpContextUtil.getIp(httpServletRequest));
            var duid = getRequestUUID(httpServletRequest);
            if (StrUtil.isEmpty(duid)) {
                duid = generateBrowserUUID((HttpServletResponse) response);
            }
            userContext.setDUid(duid);
        }
        chain.doFilter(request, response);
        userContext.clear();
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
        String uuid = UUIDUtil.generateUUIDByTimestamp();
        Cookie cookie = new Cookie(BaseMessageFilter.TOKEN_DEVICE_UUID_KEY, uuid);
        cookie.setPath("/");
        cookie.setMaxAge(3600 * 24 * 9999);
        cookie.setHttpOnly(false);
        cookie.setValue(uuid);
        response.addCookie(cookie);
        return uuid;
    }
}