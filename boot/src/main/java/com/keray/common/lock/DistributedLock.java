package com.keray.common.lock;

import java.io.Serializable;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

/**
 * @author keray
 * @date 2019/06/04 9:44
 * 分布式锁
 */
public interface DistributedLock<L extends Object> extends Serializable {
    default void tryLock(String key, Consumer<String> callback) throws InterruptedException {
        try {
            tryLock(key, callback, 60_000);
        } catch (TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    default L tryLock(String key, Consumer<String> callback, long timeout) throws InterruptedException, TimeoutException {
        L lock = tryLock(key, timeout);
        if (!Thread.currentThread().isInterrupted()) {
            execCallback(key, callback, lock);
        } else {
            throw new InterruptedException();
        }
        return lock;
    }

    L tryLock(String key, long timeout) throws InterruptedException, TimeoutException;

    default L tryLock(String key) throws InterruptedException {
        try {
            return tryLock(key, null, 60_000);
        } catch (TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    void unLock(L lock);

    default void execCallback(String key, Consumer<String> callback, L rLock) {
        if (callback != null) {
            try {
                callback.accept(key);
            } finally {
                unLock(rLock);
            }
        }
    }

}
