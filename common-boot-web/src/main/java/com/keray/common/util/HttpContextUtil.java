package com.keray.common.util;

import cn.hutool.core.util.StrUtil;
import com.keray.common.DeviceType;

import javax.servlet.http.HttpServletRequest;

/**
 * @author by keray
 * date:2021/7/16 2:48 下午
 */
public class HttpContextUtil {

    private final static String DEVICE_TYPE = "dt";
    private final static String HEADER_HOST = "ht";

    private final static String DEVICE_UUID = "duid";


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

    public static String getIp(HttpServletRequest request) {
        String forwardedIp = request.getHeader("x-forwarded-for");
        if (StrUtil.isNotBlank(forwardedIp)) {
            String[] ips = forwardedIp.split(",");
            return ips[0];
        }
        return request.getRemoteAddr();
    }
}
