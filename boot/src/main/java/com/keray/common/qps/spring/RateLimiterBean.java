package com.keray.common.qps.spring;

import com.keray.common.exception.QPSFailException;
import com.keray.common.qps.RateLimiterParams;
import com.keray.common.qps.RejectStrategy;

public interface RateLimiterBean<L> {

    /**
     * 全量参数函数
     * @param params
     * @throws InterruptedException 异常中断
     * @throws QPSFailException     QPS阻止
     */
    void acquire(RateLimiterParams params) throws QPSFailException, InterruptedException;


    /**
     * 释放令牌
     *
     * @param params
     * @throws InterruptedException
     */
    void release(RateLimiterParams params) throws InterruptedException;

}
