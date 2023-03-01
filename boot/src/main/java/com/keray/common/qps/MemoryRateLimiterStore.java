package com.keray.common.qps;

import com.keray.common.SystemProperty;
import com.keray.common.threadpool.SysThreadPool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.support.CronTrigger;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class MemoryRateLimiterStore implements RateLimiterStore {

    private final Map<String, Node> store = new ConcurrentHashMap<>();


    private final Object CLOCK = new Object();

    private final AtomicBoolean cleanFlag = new AtomicBoolean(false);

    private static final int maxLen;

    static {
        maxLen = Integer.parseInt(System.getProperty(SystemProperty.MEMORY_RATE_MAX_LEN, "1000000"));
    }

    private static class Node {
        private String value;
        private Long time;
    }

    public MemoryRateLimiterStore() {
        // 提交每天凌晨4点的定时任务
        SysThreadPool.taskScheduler.schedule(new Run(), new CronTrigger("0 0 4 * * ? "));
    }

    @Override
    public String getStoreData(String key) {
        var node = store.get(key);
        if (node == null) return null;
        return node.value;
    }

    @Override
    public void setStoreData(String key, String data) {
        var node = new Node();
        node.time = System.currentTimeMillis();
        node.value = data;
        if (cleanFlag.get()) {
            synchronized (CLOCK) {
                store.put(key, node);
            }
            return;
        }
        store.put(key, node);
        // 判断长度是否大于100w  100w  一个数据 (100byte 0.1kb)  1M*0.1kb=100M
        if (store.size() > maxLen) {
            store.clear();
        }
    }

    class Run implements Runnable {
        @Override
        public void run() {
            cleanFlag.set(true);
            // 24小时前的时间
            long now = System.currentTimeMillis() - 24 * 3600 * 1000;
            synchronized (CLOCK) {
                List<String> removeKeys = new LinkedList<>();
                for (Map.Entry<String, Node> entry : store.entrySet()) {
                    // 如果key的更新时间大于24小时 就清理掉这个key
                    if (entry.getValue().time < now) {
                        removeKeys.add(entry.getKey());
                    }
                }
                log.warn("定时清除失效的qps键值:{}", removeKeys);
                for (String key : removeKeys) {
                    store.remove(key);
                }
            }
            cleanFlag.set(false);
        }
    }
}
