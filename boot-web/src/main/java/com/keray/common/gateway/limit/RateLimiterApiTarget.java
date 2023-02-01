package com.keray.common.gateway.limit;

/**
 * 接口令牌桶限定对象
 */
public enum RateLimiterApiTarget {
    namespace, // 空间名限定 空间名限定时表示全局限制这个接口，比如一个接口只能允许10QPS/秒
    ip,// ip限定    ip限制表示
    user, // 用户id限定
}
