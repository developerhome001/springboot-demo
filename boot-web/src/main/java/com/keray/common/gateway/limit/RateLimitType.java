package com.keray.common.gateway.limit;

/**
 * 流控限制类型
 */
public enum RateLimitType {
    use, // 用户使用限制   用户使用限制不需要接口降级
    system // 系统稳定限制  系统稳定限制需要接口降级
}
