package com.keray.common.qps.spring;

import com.keray.common.exception.QPSFailException;
import com.keray.common.qps.RejectStrategy;

public interface RateLimiterBean<L> {


    default void acquire(String key, String namespace, int maxRate, int acquireCount, int millisecond, String appointCron, int recoveryCount, RejectStrategy rejectStrategy) throws QPSFailException, InterruptedException {
        acquire(key, namespace, maxRate, acquireCount, millisecond, appointCron, recoveryCount, rejectStrategy, millisecond * 5, millisecond / 20);
    }

    default void acquire(String key, String namespace, int maxRate, int acquireCount, int millisecond, int recoveryCount, RejectStrategy rejectStrategy) throws QPSFailException, InterruptedException {
        acquire(key, namespace, maxRate, acquireCount, millisecond, null, recoveryCount, rejectStrategy, millisecond * 5, millisecond / 20);
    }

    /**
     * @param key            令牌key
     * @param namespace      令牌桶空间名
     * @param maxRate        最大令牌数量
     * @param acquireCount   获取令牌数量
     * @param millisecond    下次产生令牌时间间隔（毫秒）
     * @param appointCron    在指定的Cron时间点产生令牌
     * @param recoveryCount  下次产生令牌数量
     * @param rejectStrategy 令牌限流策略
     * @param waitTime       等待时间
     * @param waitSpeed      等待时间间隔
     * @throws InterruptedException 异常中断
     * @throws QPSFailException     QPS阻止
     */
    void acquire(String key, String namespace, int maxRate, int acquireCount, int millisecond, String appointCron, int recoveryCount, RejectStrategy rejectStrategy, int waitTime, int waitSpeed) throws QPSFailException, InterruptedException;

    default void acquire(String key, String namespace, int maxRate, int acquireCount, int millisecond, String appointCron, int recoveryCount, RejectStrategy rejectStrategy, int waitTime) throws QPSFailException, InterruptedException {
        acquire(key, namespace, maxRate, acquireCount, millisecond, appointCron, recoveryCount, rejectStrategy, waitTime, waitTime / 100);
    }

    default void acquire(String key, String namespace, int maxRate, RejectStrategy rejectStrategy) throws QPSFailException, InterruptedException {
        acquire(key, namespace, maxRate, 1, 1000, null, 1, rejectStrategy, 5000, 50);
    }

    default void acquire(String key, String namespace, int maxRate, int acquireCount, RejectStrategy rejectStrategy) throws QPSFailException, InterruptedException {
        acquire(key, namespace, maxRate, acquireCount, 1000, null, 1, rejectStrategy, 5000, 50);
    }

    default void acquire(String key, String namespace, int maxRate, int acquireCount, int recoveryCount, RejectStrategy rejectStrategy) throws QPSFailException, InterruptedException {
        acquire(key, namespace, maxRate, acquireCount, 1000, null, recoveryCount, rejectStrategy, 5000, 50);
    }
}
