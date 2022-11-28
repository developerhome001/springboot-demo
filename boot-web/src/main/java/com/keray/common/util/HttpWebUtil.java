package com.keray.common.util;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public final class HttpWebUtil {


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


}
