package com.keray.common.cache;

import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.boot.autoconfigure.task.TaskSchedulingProperties;
import org.springframework.boot.task.TaskSchedulerBuilder;
import org.springframework.cache.interceptor.CacheInterceptor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.PeriodicTrigger;

import javax.annotation.PreDestroy;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class KerayCacheAspectSupport extends CacheInterceptor {

    private final static String THREAD_NAME = "auto-cache";

    public static ThreadPoolTaskScheduler taskScheduler;

    public final static ThreadLocal<Integer> TTL = new ThreadLocal<>();

    static {
        TaskSchedulerBuilder builder = new TaskSchedulerBuilder();
        builder = builder.poolSize(5);
        TaskSchedulingProperties.Shutdown shutdown = new TaskSchedulingProperties.Shutdown();
        builder = builder.awaitTermination(shutdown.isAwaitTermination());
        builder = builder.awaitTerminationPeriod(shutdown.getAwaitTerminationPeriod());
        builder = builder.threadNamePrefix(THREAD_NAME);
        taskScheduler = builder.build();
        taskScheduler.initialize();
    }

    /**
     * 最大缓存数
     */
    private final int MAX = Integer.parseInt(System.getProperty("CACHE_AUTO", "100000"));

    private final AtomicInteger count = new AtomicInteger();

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        var method = invocation.getMethod();
        var aniTime = method.getAnnotation(CacheTime.class);
        if (aniTime != null) {
            TTL.set(aniTime.value());
        }
        try {
            Object result = super.invoke(invocation);
            // 如果是自动缓存线程
            if (Thread.currentThread().getName().startsWith(THREAD_NAME)) {
                // 刷新缓存 todo
            }
            var ani = method.getAnnotation(AutoFlushCache.class);
            if (ani == null) return result;
            // 如果自增一次后大于总数
            if (count.getAndIncrement() > MAX) {
                // 自减后返回结果
                count.getAndDecrement();
                return result;
            }
            // 根据设定时间自动调用下一次
            var time = ani.time();
            taskScheduler.schedule(() -> {
                try {
                    method.invoke(invocation.getThis(), invocation.getArguments());
                } catch (IllegalAccessException | InvocationTargetException e) {
                    log.error("定时任务失败", e);
                }
                count.getAndDecrement();
            }, new PeriodicTrigger(time));
            return result;
        } finally {
            TTL.remove();
        }
    }

    @PreDestroy
    public void close() {
        taskScheduler.shutdown();
    }

}
