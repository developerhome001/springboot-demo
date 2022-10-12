package com.keray.common.qps;

import cn.hutool.core.util.StrUtil;
import com.keray.common.exception.QPSFailException;
import com.keray.common.support.DistributedLock;
import lombok.extern.slf4j.Slf4j;

/**
 * 令牌桶限流
 * 令牌桶数据格式 余量_上次产生令牌的时间戳
 */
@Slf4j
public class RateLimiter {

    public static <L> void acquire(String key, String namespace, RateLimiterStore store, int maxRate, DistributedLock<L> distributedLock, RejectStrategy rejectStrategy) throws QPSFailException, InterruptedException {
        key = namespace + key;
        privateAcquire(key, namespace, store, maxRate, distributedLock, 1, 1000, 1, rejectStrategy, 1000 * 5);
    }

    public static <L> void acquire(String key, String namespace, RateLimiterStore store, int maxRate, DistributedLock<L> distributedLock, int acquireCount, RejectStrategy rejectStrategy) throws QPSFailException, InterruptedException {
        key = namespace + key;
        privateAcquire(key, namespace, store, maxRate, distributedLock, acquireCount, 1000, 1, rejectStrategy, 1000 * 5);
    }

    public static <L> void acquire(String key, String namespace, RateLimiterStore store, int maxRate, DistributedLock<L> distributedLock, int acquireCount, int recoveryCount, RejectStrategy rejectStrategy) throws QPSFailException, InterruptedException {
        key = namespace + key;
        privateAcquire(key, namespace, store, maxRate, distributedLock, acquireCount, 1000, recoveryCount, rejectStrategy, 1000 * 5);
    }


    public static <L> void acquire(String key, String namespace, RateLimiterStore store, int maxRate, DistributedLock<L> distributedLock, int acquireCount, int millisecond, int recoveryCount, RejectStrategy rejectStrategy) throws QPSFailException, InterruptedException {
        key = namespace + key;
        privateAcquire(key, namespace, store, maxRate, distributedLock, acquireCount, millisecond, recoveryCount, rejectStrategy, millisecond * 5);
    }

    /**
     * @param key
     * @param acquireCount    获取令牌数量
     * @param namespace       令牌桶空间名
     * @param store           令牌桶数据仓库
     * @param maxRate         最大令牌数量
     * @param millisecond     下次产生令牌时间间隔（毫秒）
     * @param recoveryCount   下次产生令牌数量
     * @param rejectStrategy  令牌限流策略
     * @param distributedLock 分布式锁
     * @param waitTime        等待时间
     * @throws InterruptedException
     */
    public static <L> void acquire(String key, String namespace, RateLimiterStore store, int maxRate, DistributedLock<L> distributedLock, int acquireCount, int millisecond, int recoveryCount, RejectStrategy rejectStrategy, int waitTime) throws QPSFailException, InterruptedException {
        key = namespace + key;
        privateAcquire(key, namespace, store, maxRate, distributedLock, acquireCount, millisecond, recoveryCount, rejectStrategy, waitTime);

    }

    private static <L> void privateAcquire(String key, String namespace, RateLimiterStore store, int maxRate, DistributedLock<L> distributedLock, int acquireCount, int millisecond, int recoveryCount, RejectStrategy rejectStrategy, int waitTime) throws InterruptedException, QPSFailException {
        if (maxRate < 1 || recoveryCount < 1 || millisecond < 1) throw new RuntimeException("最大令牌数 间隔时间 下次产生令牌上不允许小于1");
        L lock = null;
        var locaRelease = false;
        try {
            if (distributedLock != null) {
                lock = distributedLock.tryLock(distributedLockKey(key));
            }
            var data = rateDataTrans(store.getStoreData(key));
            int rateBalance;
            long lastTimestamp;
            long now = System.currentTimeMillis();
            if (data == null) {
                lastTimestamp = now;
                rateBalance = initRate(store, maxRate, key, now);
            } else {
                lastTimestamp = data[0];
                rateBalance = Math.toIntExact(data[1]);
            }
            // 间隔时间大于产生间隔生成新的令牌
            var s = now - lastTimestamp;
            if (s >= millisecond) {
                var c = s / millisecond;
                // 生成间隔时间内生成的令牌
                rateBalance += recoveryCount * c;
                // 保证令牌数量只能是最大值
                rateBalance = Math.min(rateBalance, maxRate);
                lastTimestamp = now;
            }
            if (rateBalance < acquireCount) {
                if (distributedLock != null && lock != null) {
                    distributedLock.unLock(lock);
                    locaRelease = true;
                }
                // 令牌不足
                reject(key, namespace, store, maxRate, distributedLock, acquireCount, millisecond, recoveryCount, rejectStrategy, waitTime);
            } else {
                // 正常拿到令牌返回
                rateBalance -= acquireCount;
                store.setStoreData(key, rateDataTrans(lastTimestamp, rateBalance));
            }
        } finally {
            if (distributedLock != null && lock != null && !locaRelease) {
                distributedLock.unLock(lock);
            }
        }
    }

    /**
     * 令牌获取失败
     *
     * @param key
     */
    private static <L> void reject(String key, String namespace, RateLimiterStore store, int maxRate, DistributedLock<L> distributedLock, int acquireCount, int millisecond, int recoveryCount, RejectStrategy rejectStrategy, int waitTime) throws InterruptedException, QPSFailException {
        log.warn("RateLimiter warn:{}", key);
        if (rejectStrategy == RejectStrategy.noting) return;
        if (rejectStrategy == RejectStrategy.throw_exception) throw new QPSFailException();
        if (rejectStrategy == RejectStrategy.wait) {
            for (var i = 0; i < waitTime / 100; i++) {
                Thread.sleep(100);
                try {
                    privateAcquire(key, namespace, store, maxRate, distributedLock, acquireCount, millisecond, recoveryCount, RejectStrategy.throw_exception, waitTime);
                    return;
                } catch (QPSFailException ignore) {
                }
            }
        }
        throw new QPSFailException();
    }

    /**
     * 初始化令牌桶
     *
     * @param key
     * @param timestamp
     * @return
     */
    private static int initRate(RateLimiterStore store, int maxRate, String key, long timestamp) {
        var data = new Long[]{timestamp, (long) maxRate};
        store.setStoreData(key, rateDataTrans(data[0], Math.toIntExact(data[1])));
        return maxRate;
    }

    private static Long[] rateDataTrans(String storeData) {
        if (StrUtil.isEmpty(storeData)) return null;
        var s = storeData.split("_");
        return new Long[]{Long.parseLong(s[0]), Long.parseLong(s[1])};
    }

    private static String rateDataTrans(Long timestamp, int rateCount) {
        return String.format("%d_%d", timestamp, rateCount);
    }


    private static String distributedLockKey(String key) {
        return "RateLimiter:" + key;
    }

}
