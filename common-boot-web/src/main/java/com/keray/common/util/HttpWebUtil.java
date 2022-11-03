package com.keray.common.util;

import cn.hutool.core.util.StrUtil;
import com.keray.common.DeviceType;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public final class HttpWebUtil {

    private final static String DEVICE_TYPE = "dt";
    private final static String HEADER_HOST = "ht";

    private final static String DEVICE_UUID = "duid";

    public static String getCookieValue(HttpServletRequest request, String key) {
        return getCookieValue(request.getCookies(), key);
    }

    public static String getCookieValue(Cookie[] cookies, String key) {
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (cookie.getName().equals(key)) {
                return cookie.getValue();
            }
        }
        return null;
    }

    public static void addCookie(HttpServletResponse response, String key, String value) {
        addCookie(response, key, value, -1);
    }

    public static void addCookie(HttpServletResponse response, String key, String value, int expiry) {
        Cookie cookie = new Cookie(key, value);
        cookie.setPath("/");
        cookie.setMaxAge(expiry);
        response.addCookie(cookie);

    }




    public static DeviceType currentDeviceType(HttpServletRequest request) {
        String vl = request.getHeader(DEVICE_TYPE);
        if (StrUtil.isBlank(vl)) {
            return DeviceType.unknown;
        }
        return DeviceType.valueOf(vl);
    }

    public static String host(HttpServletRequest request) {
        return request.getHeader(HEADER_HOST);
    }

    public static String duuid(HttpServletRequest request) {
        return HttpWebUtil.getCookieValue(request, DEVICE_UUID);
    }
}
