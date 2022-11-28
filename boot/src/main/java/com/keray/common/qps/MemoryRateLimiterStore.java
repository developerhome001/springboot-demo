package com.keray.common.qps;

import com.keray.common.SysThreadPool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.support.CronTrigger;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Slf4j
public class MemoryRateLimiterStore implements RateLimiterStore {

    private final Map<String, String> store = new HashMap<>();

    /**
     * 统计每个key的最新时间
     * 利用守护线程定时清理长期未用的key
     */
    private final Map<String, Long> storeTime = new HashMap<>();

    private final Object CLOCK = new Object();

    private volatile boolean cleanFlag = false;

    public MemoryRateLimiterStore() {
        // 提交每天凌晨4点的定时任务
        SysThreadPool.taskScheduler.schedule(new Run(), new CronTrigger("0 0 4 * * ? "));
    }

    @Override
    public String getStoreData(String key) {
        return store.get(key);
    }

    @Override
    public void setStoreData(String key, String data) {
        if (cleanFlag) {
            synchronized (CLOCK) {
                store.put(key, data);
                storeTime.put(key, System.currentTimeMillis());
            }
            return;
        }
        store.put(key, data);
        storeTime.put(key, System.currentTimeMillis());
    }

    class Run implements Runnable {

        @Override
        public void run() {
            cleanFlag = true;
            // 24小时前的时间
            long now = System.currentTimeMillis() - 24 * 3600 * 1000;
            synchronized (CLOCK) {
                List<String> removeKeys = new LinkedList<>();
                for (Map.Entry<String, Long> entry : storeTime.entrySet()) {
                    // 如果key的更新时间大于24小时 就清理掉这个key
                    if (entry.getValue() < now) {
                        removeKeys.add(entry.getKey());
                    }
                }
                log.warn("定时清除失效的qps键值:{}", removeKeys);
                for (String key : removeKeys) {
                    store.remove(key);
                    storeTime.remove(key);
                }
            }
            cleanFlag = false;
        }
    }

    public Map<String, String> getStore() {
        return store;
    }

    public Map<String, Long> getStoreTime() {
        return storeTime;
    }
}
