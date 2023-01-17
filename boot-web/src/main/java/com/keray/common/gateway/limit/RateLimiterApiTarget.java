package com.keray.common.gateway.limit;

/**
 * 接口令牌桶限定对象
 */
public enum RateLimiterApiTarget {
    namespace, // 空间名限定
    ip,// ip限定
    user, // 用户id限定
}
