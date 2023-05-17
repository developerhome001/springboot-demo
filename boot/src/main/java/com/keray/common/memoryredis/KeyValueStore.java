package com.keray.common.memoryredis;

import com.keray.common.threadpool.MemoryLimitCalculator;
import com.keray.common.threadpool.SysThreadPool;
import org.springframework.scheduling.support.CronTrigger;

import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;

/**
 * kv存储
 *
 * @param <K>
 * @param <V>
 */
public class KeyValueStore<K, V> {
    /**
     *
     */
    public final ConcurrentHashMap<K, Node<K, V>> map = new ConcurrentHashMap<>();

    /**
     * 优先队列  清除最近过期的key
     */
    private final PriorityQueue<Node<K, V>> queue = new PriorityQueue();

    private static final int THE_512_GB = 512 * 1024 * 1024;

    private final int maxFreeMemory;

    public KeyValueStore() {
        this(THE_512_GB);
    }

    public KeyValueStore(int maxFreeMemory) {
        this.maxFreeMemory = maxFreeMemory;
        // 每个整点清除过期的key
        SysThreadPool.taskScheduler.schedule(this::expireOldest, new CronTrigger("0 0 0/1 * * ? "));
    }


    /**
     * @param key
     * @param value
     * @param expireTime 多少毫秒后过期
     */
    public void put(K key, V value, long expireTime) {
        var node = new Node<>(key, long2time(System.currentTimeMillis() + expireTime), value);
        // 判断内存
        if (!hasRemainedMemory()) {
            // 内存不够  进行最近过期释放
            expireOldest();
        }
        map.put(key, node);
        synchronized (this) {
            queue.offer(node);
        }

    }

    public V get(K key) {
        var v = map.get(key);
        if (v == null) return null;
        if (isExpired(v)) {
            map.remove(key);
            return null;
        }
        return v.value;
    }

    public V remove(K key) {
        var n = map.remove(key);
        if (n != null)
            return n.value;
        return null;
    }

    public boolean containsKey(K key) {
        return get(key) != null;
    }

    private boolean isExpired(Node node) {
        var now = System.currentTimeMillis();
        return now > time2long(node.time, now);
    }

    private long time2long(int time, long ms) {
        return ms / 100000000000L * 100000000000L + time * 1000L;
    }

    private static int long2time(long time) {
        time = time / 1000;
        var x = time / 100000000 * 100000000;
        return (int) (time % x);
    }

    public boolean hasRemainedMemory() {
        return MemoryLimitCalculator.maxAvailable() > maxFreeMemory;
    }

    private void expireOldest() {
        synchronized (this) {
            while (!queue.isEmpty()) {
                var node = queue.poll();
                if (!isExpired(node)) {
                    return;
                }
                map.remove(node.key);
            }
        }
    }

    private static class Node<K, V> implements Comparable<Node<K, V>> {

        public Node(K key, int time, V value) {
            this.key = key;
            this.time = time;
            this.value = value;
        }

        private final K key;

        /**
         * 过期时间
         * 秒级
         * 去掉时间戳最前面两位  保留8位数
         * 16 77822468  422
         */
        private final int time;

        /**
         * value
         */
        private final V value;

        @Override
        public int compareTo(Node<K, V> o) {
            return Integer.compare(time, o.time);
        }
    }

    public static void main(String[] args) throws InterruptedException {
        var store = new KeyValueStore();
        store.put("123", "asdfd", 1000);
        System.out.println(store.remove("123"));
        for (; ; ) {
            Thread.sleep(100);
            var val = store.get("123");
            if (val == null) break;
            System.out.println(val);
        }
        System.out.println("end");
    }
}
