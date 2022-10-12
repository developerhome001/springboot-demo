package com.keray.common;

/**
 * 接口令牌桶限定对象
 */
public enum RateLimiterApiTarget {
    namespace, // 空间名限定
    ip,// ip限定
    duid,// duid限定
    user, // 用户id限定
    userOrDuid, // 用户id限定
}
