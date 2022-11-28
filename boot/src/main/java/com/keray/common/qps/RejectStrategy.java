package com.keray.common.qps;

/**
 * 令牌桶ps限制后的执行策略
 */
public enum RejectStrategy {
    //拒绝抛出异常
    throw_exception,
    //等待qps降低到限制一下执行
    wait,
    // ai验证
    ai_verification,
    //不做任何操作
    noting,
}
