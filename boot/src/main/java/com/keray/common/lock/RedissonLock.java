package com.keray.common.lock;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author by keray
 * date:2020/1/10 10:00 AM
 */
@Slf4j
public class RedissonLock implements DistributedLock<RLock>, ApplicationContextAware {

    private final RedissonClient redissonClient;

    private static ApplicationContext applicationContext;


    public RedissonLock(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
        log.info("注入：RedissonLock");
    }

    @Override
    public RLock tryLock(String key, long timeout) throws InterruptedException, TimeoutException {
        String clockKey = "redis:clock:" + key;
        RLock rLock = redissonClient.getLock(clockKey);
        if (rLock.tryLock(timeout, TimeUnit.MILLISECONDS)) {
            return rLock;
        }
        throw new TimeoutException();
    }


    @Override
    public void unLock(RLock lock) {
        if (lock != null) {
            lock.unlock();
        }
    }


    private static DistributedLock<RLock> distributedLock;

    public static RLock lock(String key, Consumer<String> callback) throws TimeoutException, InterruptedException {
        if (distributedLock == null) {
            synchronized (RedissonLock.class) {
                if (distributedLock == null) {
                    distributedLock = applicationContext.getBean(RedissonLock.class);
                }
            }
        }
        return distributedLock.tryLock(key, callback, 60_000);
    }

    public static <T> T lock(String key, Supplier<T> callback) throws TimeoutException, InterruptedException {
        if (distributedLock == null) {
            synchronized (RedissonLock.class) {
                if (distributedLock == null) {
                    distributedLock = applicationContext.getBean(RedissonLock.class);
                }
            }
        }
        RLock lock = null;
        try {
            lock = distributedLock.tryLock(key, 60_000);
            return callback.get();
        } finally {
            if (lock != null) {
                lock.unlock();
            }
        }
    }

    public static RLock lock(String key) throws TimeoutException, InterruptedException {
        if (distributedLock == null) {
            synchronized (RedissonLock.class) {
                if (distributedLock == null) {
                    distributedLock = applicationContext.getBean(RedissonLock.class);
                }
            }
        }
        return distributedLock.tryLock(key, 60_000);
    }

    public static void unlock(RLock lock) {
        distributedLock.unLock(lock);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        RedissonLock.applicationContext = applicationContext;
    }
}
