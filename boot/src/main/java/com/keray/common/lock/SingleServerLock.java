package com.keray.common.lock;

import com.keray.common.threadpool.SysThreadPool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.support.CronTrigger;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

@Slf4j
public class SingleServerLock implements DistributedLock<String> {

    private final ConcurrentHashMap<String, Integer> data = new ConcurrentHashMap<>();

    private static final SingleServerLock instance = new SingleServerLock();

    public SingleServerLock() {
        // 调试日志 每个小时打印下map的锁长度 好排除忘记释放的锁
        SysThreadPool.taskScheduler.schedule(() -> {
            List<String> keys = new LinkedList<>(data.keySet());
            if (!keys.isEmpty()) {
                log.warn("单机分布式锁当前锁定keys={}", keys);
            }
        }, new CronTrigger("0 0 0/1 * * ? "));
    }

    public static SingleServerLock get() {
        return instance;
    }

    @Override
    public String tryLock(String key, long timeout) throws InterruptedException, TimeoutException {
        int threadCode = Thread.currentThread().hashCode();
        // 加锁
        if (data.computeIfAbsent(key, k -> threadCode) == threadCode) return key;
        // 加锁失败  开始轮训
        long now = System.currentTimeMillis();
        for (; ; ) {
            long nowx = System.currentTimeMillis();
            if (nowx - now > timeout) throw new TimeoutException();
            if (data.computeIfAbsent(key, k -> threadCode) == threadCode) return key;
        }
    }

    @Override
    public void unLock(String lock) {
        data.remove(lock);
    }

}