package com.keray.common.qps;

import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.cron.CronUtil;
import cn.hutool.json.JSONUtil;
import com.keray.common.exception.QPSFailException;
import com.keray.common.lock.DistributedLock;
import com.keray.common.utils.TimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.support.CronExpression;

import java.time.LocalDateTime;

/**
 * 令牌桶限流
 * 令牌桶数据格式 余量_上次产生令牌的时间戳
 */
@Slf4j
public class RateLimiter {

    public static <L> void acquire(String key, String namespace, RateLimiterStore store, int maxRate, DistributedLock<L> distributedLock, RejectStrategy rejectStrategy) throws QPSFailException, InterruptedException {
        key = namespace + key;
        privateAcquire(key, namespace, store, maxRate, distributedLock, 1, 1000, null, 1, rejectStrategy, 1000 * 5);
    }

    public static <L> void acquire(String key, String namespace, RateLimiterStore store, int maxRate, DistributedLock<L> distributedLock, int acquireCount, RejectStrategy rejectStrategy) throws QPSFailException, InterruptedException {
        key = namespace + key;
        privateAcquire(key, namespace, store, maxRate, distributedLock, acquireCount, 1000, null, 1, rejectStrategy, 1000 * 5);
    }

    public static <L> void acquire(String key, String namespace, RateLimiterStore store, int maxRate, DistributedLock<L> distributedLock, int acquireCount, int recoveryCount, RejectStrategy rejectStrategy) throws QPSFailException, InterruptedException {
        key = namespace + key;
        privateAcquire(key, namespace, store, maxRate, distributedLock, acquireCount, 1000, null, recoveryCount, rejectStrategy, 1000 * 5);
    }


    public static <L> void acquire(String key, String namespace, RateLimiterStore store, int maxRate, DistributedLock<L> distributedLock, int acquireCount, int millisecond, int recoveryCount, RejectStrategy rejectStrategy) throws QPSFailException, InterruptedException {
        key = namespace + key;
        privateAcquire(key, namespace, store, maxRate, distributedLock, acquireCount, millisecond, null, recoveryCount, rejectStrategy, millisecond * 5);
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
    public static <L> void acquire(String key, String namespace, RateLimiterStore store, int maxRate, DistributedLock<L> distributedLock, int acquireCount, int millisecond, String appointCron, int recoveryCount, RejectStrategy rejectStrategy, int waitTime) throws QPSFailException, InterruptedException {
        key = namespace + key;
        privateAcquire(key, namespace, store, maxRate, distributedLock, acquireCount, millisecond, appointCron, recoveryCount, rejectStrategy, waitTime);

    }

    private static <L> void privateAcquire(String key, String namespace, RateLimiterStore store, int maxRate, DistributedLock<L> distributedLock, int acquireCount, int millisecond, String appointCron, int recoveryCount, RejectStrategy rejectStrategy, int waitTime) throws InterruptedException, QPSFailException {
        if (maxRate < 1 || recoveryCount < 1 || millisecond < 1)
            throw new RuntimeException("最大令牌数 间隔时间 下次产生令牌上不允许小于1");
        L lock = null;
        var locaRelease = false;
        try {
            // 分布式锁锁定当前的key
            if (distributedLock != null) {
                lock = distributedLock.tryLock(distributedLockKey(key));
            }
            // 将格式的存储数据解码 long[上次释放令牌时间，令牌剩余数量 ]
            var data = rateDataTrans(store.getStoreData(key));
            int rateBalance;
            long lastTimestamp;
            long now = System.currentTimeMillis();
            // 如果存储桶没有令牌数据 初始化令牌桶
            if (data == null) {
                lastTimestamp = now;
                rateBalance = initRate(store, maxRate, key, now);
            } else {
                lastTimestamp = data[0];
                rateBalance = Math.toIntExact(data[1]);
            }
            // 在设置的时间空隙释放令牌
            var newRate = createdNewRate(now, lastTimestamp, millisecond, appointCron, recoveryCount, rateBalance, maxRate);
            lastTimestamp = newRate[0].longValue();
            rateBalance = newRate[1].intValue();
            // 令牌剩余量小于需求量
            if (rateBalance < acquireCount) {
                if (distributedLock != null && lock != null) {
                    distributedLock.unLock(lock);
                    locaRelease = true;
                }
                // 令牌不足
                reject(key, namespace, store, maxRate, distributedLock, acquireCount, millisecond, appointCron, recoveryCount, rejectStrategy, waitTime);
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
    private static <L> void reject(String key, String namespace, RateLimiterStore store, int maxRate, DistributedLock<L> distributedLock, int acquireCount, int millisecond, String appointCron, int recoveryCount, RejectStrategy rejectStrategy, int waitTime) throws InterruptedException, QPSFailException {
        log.warn("RateLimiter warn:{}", key);
        if (key.equals("system:14.111.48.40")) {
            log.warn("调试qps存储数据:{} {} {} {}", store.getStoreData(key), maxRate, millisecond, acquireCount);
            log.warn("调试qps存储数据:{}", JSONUtil.toJsonStr(((MemoryRateLimiterStore) store).getStore()));
        } else {
            log.warn("qps失败     调试qps存储数据:{}", store.getStoreData(key));
        }
        if (rejectStrategy == RejectStrategy.noting) return;
        if (rejectStrategy == RejectStrategy.throw_exception) throw new QPSFailException();
        if (rejectStrategy == RejectStrategy.wait) {
            for (var i = 0; i < waitTime / 100; i++) {
                Thread.sleep(100);
                try {
                    privateAcquire(key, namespace, store, maxRate, distributedLock, acquireCount, millisecond, appointCron, recoveryCount, RejectStrategy.throw_exception, waitTime);
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

    /**
     * 解码存储数据
     *
     * @param storeData
     * @return
     */
    private static Long[] rateDataTrans(String storeData) {
        if (StrUtil.isEmpty(storeData)) return null;
        var s = storeData.split("_");
        return new Long[]{Long.parseLong(s[0]), Long.parseLong(s[1])};
    }

    /**
     * 编码令牌桶数据
     *
     * @param timestamp
     * @param rateCount
     * @return
     */
    private static String rateDataTrans(Long timestamp, int rateCount) {
        return String.format("%d_%d", timestamp, rateCount);
    }

    /**
     * 产生新令牌
     *
     * @return
     */
    private static Number[] createdNewRate(long now, long lastTimestamp, long millisecond, String appointCron, int recoveryCount, int rateBalance, int maxRate) {
        // 如果指定时间恢复令牌
        long c = 0;
        if (StrUtil.isNotBlank(appointCron)) {
            var lastTime = LocalDateTimeUtil.of(lastTimestamp);
            var nowTime = LocalDateTimeUtil.of(now);
            // 寻找cron表示最近的上下两个时间点
            var ex = CronExpression.parse(appointCron);
            // 获取上一次执行的下一个执行点  如果当前时间大于执行点  开始产生令牌
            var next = ex.next(lastTime);
            for (; next != null && nowTime.isAfter(next); c++) {
                next = ex.next(next);
            }
        } else {
            var s = now - lastTimestamp;
            if (s >= millisecond) {
                c = s / millisecond;
            }
        }
        if (c > 0) {
            // 生成间隔时间内生成的令牌
            rateBalance += recoveryCount * c;
            // 保证令牌数量只能是最大值
            rateBalance = Math.min(rateBalance, maxRate);
            lastTimestamp = now;
        }
        return new Number[]{lastTimestamp, rateBalance};
    }


    private static String distributedLockKey(String key) {
        return "RateLimiter:" + key;
    }

    public static void main(String[] args) {
        var ex = CronExpression.parse("0 0 0 * * ? ");
        System.out.println(ex.next(LocalDateTime.parse("2023-01-18 00:00:00", TimeUtil.DATE_TIME_FORMATTER_SC)));
        System.out.println(ex.next(LocalDateTime.now()));
    }
}
