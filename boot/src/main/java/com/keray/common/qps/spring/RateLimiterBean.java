package com.keray.common.qps.spring;

import com.keray.common.exception.QPSFailException;
import com.keray.common.qps.RejectStrategy;

public interface RateLimiterBean<L> {


    /**
     * 使用Cron时间产生令牌并且默认等待时间和等待时间间隔
     */
    default void acquire(String key, String namespace, int maxRate, int acquireCount, String appointCron, int recoveryCount, RejectStrategy rejectStrategy) throws QPSFailException, InterruptedException {
        acquire(key, namespace, maxRate, acquireCount, 0, appointCron, recoveryCount, rejectStrategy, 5000, 50);
    }

    /**
     * 使用时间点产生新令牌并且默认等待时间和等待时间间隔
     */
    default void acquire(String key, String namespace, int maxRate, int acquireCount, int millisecond, int recoveryCount, RejectStrategy rejectStrategy) throws QPSFailException, InterruptedException {
        acquire(key, namespace, maxRate, acquireCount, millisecond, null, recoveryCount, rejectStrategy, millisecond * 5, millisecond / 20);
    }

    /**
     * 全量参数函数
     *
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
     * @param needRelease    是否是可以释放的令牌桶  可以释放的令牌桶不会自己生成令牌
     * @throws InterruptedException 异常中断
     * @throws QPSFailException     QPS阻止
     */
    void acquire(String key, String namespace, int maxRate, int acquireCount, int millisecond, String appointCron, int recoveryCount, RejectStrategy rejectStrategy, int waitTime, int waitSpeed, boolean needRelease) throws QPSFailException, InterruptedException;

    /**
     * 不需要释放的令牌桶全量参数函数
     */
    default void acquire(String key, String namespace, int maxRate, int acquireCount, int millisecond, String appointCron, int recoveryCount, RejectStrategy rejectStrategy, int waitTime, int waitSpeed) throws QPSFailException, InterruptedException {
        acquire(key, namespace, maxRate, acquireCount, millisecond, appointCron, recoveryCount, rejectStrategy, waitTime, waitSpeed, false);
    }


    /**
     * 可以设置等待时间且默认等待时间间隔
     */
    default void acquire(String key, String namespace, int maxRate, int acquireCount, int millisecond, String appointCron, int recoveryCount, RejectStrategy rejectStrategy, int waitTime) throws QPSFailException, InterruptedException {
        acquire(key, namespace, maxRate, acquireCount, millisecond, appointCron, recoveryCount, rejectStrategy, waitTime, waitTime / 100);
    }

    /**
     * 默认一次获取1个令牌 默认1秒后产生1个令牌，并且默认等待5000毫秒  等待间隔50毫秒
     */
    default void acquire(String key, String namespace, int maxRate, RejectStrategy rejectStrategy) throws QPSFailException, InterruptedException {
        acquire(key, namespace, maxRate, 1, 1000, null, 1, rejectStrategy, 5000, 50);
    }

    /**
     * 可以设置每次获取的令牌数量 默认1秒后产生1个令牌 默认等待5000毫秒  等待间隔50毫秒
     */
    default void acquire(String key, String namespace, int maxRate, int acquireCount, RejectStrategy rejectStrategy) throws QPSFailException, InterruptedException {
        acquire(key, namespace, maxRate, acquireCount, 1000, null, 1, rejectStrategy, 5000, 50);
    }


    /**
     * 可以设置每次获取的令牌数和下一次产生的令牌数 默认1秒后产生新令牌 默认等待5000毫秒  等待间隔50毫秒
     */
    default void acquire(String key, String namespace, int maxRate, int acquireCount, int recoveryCount, RejectStrategy rejectStrategy) throws QPSFailException, InterruptedException {
        acquire(key, namespace, maxRate, acquireCount, 1000, null, recoveryCount, rejectStrategy, 5000, 50);
    }


    /**
     * 使用下一个时间点产生令牌，只默认了等待时间间隔
     */
    default void acquire(String key, String namespace, int maxRate, int acquireCount, int millisecond, int recoveryCount, RejectStrategy rejectStrategy, int waitTime) throws QPSFailException, InterruptedException {
        acquire(key, namespace, maxRate, acquireCount, millisecond, null, recoveryCount, rejectStrategy, waitTime);
    }

    /**
     * 可释放型令牌桶全量参数
     */
    default void releaseAcquire(String key, String namespace, int maxRate, int acquireCount, RejectStrategy rejectStrategy, int waitTime, int waitSpeed) throws QPSFailException, InterruptedException {
        // 下一个释放时间间隔和释放数量随便设置大于0的数字都行
        acquire(key, namespace, maxRate, acquireCount, 1, null, 1, rejectStrategy, waitTime, waitSpeed, true);
    }


    /**
     * 释放令牌
     *
     * @param key        令牌key
     * @param namespace  令牌桶空间名
     * @param maxRate    最大令牌数量
     * @param releaseCnt 释放令牌数量
     */
    void release(String key, String namespace, Integer maxRate, int releaseCnt) throws InterruptedException;

    /**
     * 释放令牌 默认释放1个 不设置最大释放数量
     */
    default void release(String key, String namespace) throws InterruptedException {
        release(key, namespace, null, 1);
    }
}
