package com.keray.common.qps.spring;

import com.keray.common.exception.QPSFailException;
import com.keray.common.qps.RejectStrategy;

public interface RateLimiterBean<L> {


    default void acquire(String key, String namespace, int maxRate, int acquireCount, int millisecond, String appointCron, int recoveryCount, RejectStrategy rejectStrategy) throws QPSFailException, InterruptedException {
        acquire(key, namespace, maxRate, acquireCount, millisecond, appointCron, recoveryCount, rejectStrategy, millisecond * 5);
    }

    default void acquire(String key, String namespace, int maxRate, int acquireCount, int millisecond, int recoveryCount, RejectStrategy rejectStrategy) throws QPSFailException, InterruptedException {
        acquire(key, namespace, maxRate, acquireCount, millisecond, null, recoveryCount, rejectStrategy, millisecond * 5);
    }

    void acquire(String key, String namespace, int maxRate, int acquireCount, int millisecond, String appointCron, int recoveryCount, RejectStrategy rejectStrategy, int waitTime) throws QPSFailException, InterruptedException;

    default void acquire(String key, String namespace, int maxRate, RejectStrategy rejectStrategy) throws QPSFailException, InterruptedException {
        acquire(key, namespace, maxRate, 1, 1000, null, 1, rejectStrategy, 5000);
    }

    default void acquire(String key, String namespace, int maxRate, int acquireCount, RejectStrategy rejectStrategy) throws QPSFailException, InterruptedException {
        acquire(key, namespace, maxRate, acquireCount, 1000, null, 1, rejectStrategy, 5000);
    }

    default void acquire(String key, String namespace, int maxRate, int acquireCount, int recoveryCount, RejectStrategy rejectStrategy) throws QPSFailException, InterruptedException {
        acquire(key, namespace, maxRate, acquireCount, 1000, null, recoveryCount, rejectStrategy, 5000);
    }
}
