package com.keray.common.qps;

import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.keray.common.exception.QPSFailException;
import com.keray.common.lock.DistributedLock;
import com.keray.common.utils.TimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.redisson.client.RedisException;
import org.springframework.scheduling.support.CronExpression;

import java.time.LocalDateTime;

/**
 * 令牌桶限流
 * 令牌桶数据格式 余量_上次产生令牌的时间戳
 */
@Slf4j
public class RateLimiter {

    /**
     * @param key             令牌key
     * @param namespace       令牌桶空间名
     * @param store           令牌桶数据仓库
     * @param maxRate         最大令牌数量
     * @param distributedLock 分布式锁  单机的情况下就可以不要分布式锁（还得保证一个key的操作是单线程的）
     * @param acquireCount    获取令牌数量
     * @param millisecond     下次产生令牌时间间隔（毫秒）
     * @param appointCron     在指定的Cron时间点产生令牌
     * @param recoveryCount   下次产生令牌数量
     * @param rejectStrategy  令牌限流策略
     * @param waitTime        等待时间
     * @param waitSpeed       等待时间间隔
     * @param needRelease     是否是可以释放的令牌桶  可以释放的令牌桶不会自己生成令牌
     * @param <L>
     * @throws InterruptedException 异常中断
     * @throws QPSFailException     QPS阻止
     */
    public static <L> void acquire(String key,
                                   String namespace,
                                   RateLimiterStore store,
                                   int maxRate,
                                   DistributedLock<L> distributedLock,
                                   int acquireCount,
                                   int millisecond,
                                   String appointCron,
                                   int recoveryCount,
                                   RejectStrategy rejectStrategy,
                                   int waitTime,
                                   int waitSpeed,
                                   boolean needRelease
    ) throws QPSFailException, InterruptedException {
        privateAcquire(generateUUid(key, namespace), store, maxRate, distributedLock, acquireCount, millisecond, appointCron, recoveryCount, rejectStrategy, waitTime, waitSpeed, false, needRelease);

    }

    /**
     * 获取令牌的具体实现
     *
     * @param uuid            令牌uuid
     * @param store           令牌桶数据仓库
     * @param maxRate         最大令牌数量
     * @param distributedLock 分布式锁
     * @param acquireCount    获取令牌数量
     * @param millisecond     下次产生令牌时间间隔（毫秒）
     * @param appointCron     在指定的Cron时间点产生令牌
     * @param recoveryCount   下次产生令牌数量
     * @param rejectStrategy  令牌限流策略
     * @param waitTime        等待时间
     * @param waitSpeed       等待时间间隔
     * @param haveLock        是否已经加锁
     * @param needRelease     是否是可以释放的令牌桶  可以释放的令牌桶不会自己生成令牌
     * @param <L>
     * @throws InterruptedException 异常中断
     * @throws QPSFailException     QPS阻止
     */
    private static <L> void privateAcquire(String uuid,
                                           RateLimiterStore store,
                                           int maxRate,
                                           DistributedLock<L> distributedLock,
                                           int acquireCount,
                                           int millisecond,
                                           String appointCron,
                                           int recoveryCount,
                                           RejectStrategy rejectStrategy,
                                           int waitTime,
                                           int waitSpeed,
                                           boolean haveLock,
                                           boolean needRelease
    ) throws InterruptedException, QPSFailException {
        if (maxRate < 1 || recoveryCount < 1 || millisecond < 1)
            throw new RuntimeException("最大令牌数 间隔时间 下次产生令牌上不允许小于1");
        L lock = null;
        try {
            // 分布式锁锁定当前的key
            // 如果已经加锁过 不在加锁  加过锁只有在等待时递归调用会出现
            if (!haveLock && distributedLock != null) {
                lock = distributedLock.tryLock(distributedLockKey(uuid));
            }
            // 将格式的存储数据解码 long[上次释放令牌时间，令牌剩余数量 ]
            var data = rateDataTrans(store.getStoreData(uuid));
            int rateBalance;
            long lastTimestamp;
            long now = 0;
            if (!needRelease) now = System.currentTimeMillis();
            // 如果存储桶没有令牌数据 初始化令牌桶
            if (data == null) {
                lastTimestamp = now;
                rateBalance = initRate(store, maxRate, uuid, now);
            } else {
                lastTimestamp = data[0];
                rateBalance = Math.toIntExact(data[1]);
            }
            // 非释放型令牌桶才能自己产生新令牌
            if (!needRelease) {
                // 在设置的时间空隙释放令牌
                var newRate = createdNewRate(now, lastTimestamp, millisecond, appointCron, recoveryCount, rateBalance, maxRate);
                lastTimestamp = newRate[0].longValue();
                rateBalance = newRate[1].intValue();
            }
            // 令牌剩余量小于需求量
            // 进入递归重新获取令牌的阶段一直锁定当前key没问题
            // 最先来的请求都没拿到令牌，后面来的接口肯定也拿不到令牌，就直接让后来的接口等待分布式锁释放后表示可能有令牌了
            if (rateBalance < acquireCount) {
                // 令牌不足
                reject(uuid, store, maxRate, distributedLock, acquireCount, millisecond, appointCron, recoveryCount, rejectStrategy, waitTime, waitSpeed, needRelease);
            } else {
                // 正常拿到令牌返回
                rateBalance -= acquireCount;
                store.setStoreData(uuid, rateDataTrans(lastTimestamp, rateBalance));
            }
        } finally {
            if (lock != null) {
                try {
                    distributedLock.unLock(lock);
                } catch (Throwable ignore) {
                }
            }
        }
    }


    /**
     * 释放令牌
     *
     * @param key             令牌key
     * @param namespace       令牌桶空间名
     * @param maxRate         最大令牌数量
     * @param store           令牌桶数据仓库
     * @param distributedLock 分布式锁
     * @param releaseCnt      释放令牌个数  一般情况都是1
     * @param <L>
     * @throws InterruptedException
     */
    public static <L> void release(String key, String namespace, RateLimiterStore store, Integer maxRate, DistributedLock<L> distributedLock, int releaseCnt) throws InterruptedException {
        var uuid = generateUUid(key, namespace);
        L lock = null;
        try {
            // 写操作先获取分布式锁
            if (distributedLock != null) {
                lock = distributedLock.tryLock(distributedLockKey(uuid));
            }
            // 获取以前的数据
            var data = rateDataTrans(store.getStoreData(uuid));
            // 是否阶段如果data为null 唯一的可能就是使用redis存储释放间隔太长导致数据失效了
            // 如果失效了的话直接不处理  理论上不可能出现
            if (data == null) return;
            var lastTimestamp = data[0] + 1;
            // 获取剩余令牌加上释放令牌数量
            var rateBalance = Math.toIntExact(data[1]) + releaseCnt;
            // 释放令牌 理论上释放的令牌加上剩余令牌数不可能超过最大数量 只有写错了每次获取1个 释放2个的情况
            // 令牌释放时不修改上次令牌获取时间  maxRate不传的情况下不处理
            store.setStoreData(uuid, rateDataTrans(lastTimestamp, maxRate != null ? Math.min(rateBalance, maxRate) : rateBalance));
        } finally {
            if (lock != null) {
                try {
                    distributedLock.unLock(lock);
                } catch (Throwable ignore) {
                }
            }
        }
    }

    /**
     * 令牌获取失败
     *
     * @param uuid
     */
    private static <L> void reject(String uuid,
                                   RateLimiterStore store,
                                   int maxRate,
                                   DistributedLock<L> distributedLock,
                                   int acquireCount,
                                   int millisecond,
                                   String appointCron,
                                   int recoveryCount,
                                   RejectStrategy rejectStrategy,
                                   int waitTime,
                                   int waitSpeed,
                                   boolean needRelease
    ) throws InterruptedException, QPSFailException {
        if (rejectStrategy == RejectStrategy.noting) return;
        if (rejectStrategy == RejectStrategy.throw_exception) throw new QPSFailException();
        if (rejectStrategy == RejectStrategy.wait && waitTime > 0) {
            Thread.sleep(waitSpeed);
            privateAcquire(uuid, store, maxRate, distributedLock,
                    acquireCount, millisecond, appointCron, recoveryCount,
                    rejectStrategy, waitTime - waitSpeed, waitSpeed,
                    true, needRelease);
            return;
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

    private static String generateUUid(String key, String namespace) {
        return String.format("%s:%s", namespace, key);
    }

    public static void main(String[] args) {
        var ex = CronExpression.parse("0 0 0 * * ? ");
        System.out.println(ex.next(LocalDateTime.parse("2023-01-18 00:00:00", TimeUtil.DATE_TIME_FORMATTER_SC)));
        System.out.println(ex.next(LocalDateTime.now()));
    }
}
