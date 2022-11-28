package com.keray.common;

import com.keray.common.threadpool.MemorySafeLinkedBlockingQueue;
import org.springframework.boot.autoconfigure.task.TaskSchedulingProperties;
import org.springframework.boot.task.TaskSchedulerBuilder;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author by keray
 * date:2019/9/16 11:49
 */
public class SysThreadPool {
    private static final AtomicInteger COUNT = new AtomicInteger(0);

    private static final ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(50, 1000, 10,
            TimeUnit.SECONDS, new MemorySafeLinkedBlockingQueue<>(16 * 1024 * 1024),
            r -> {
                Thread t = new Thread(r);
                t.setName("sys-thread-" + COUNT.getAndIncrement());
                return t;
            });

    public static ThreadPoolTaskScheduler taskScheduler;

    public static IContext userContext;

    static {
        TaskSchedulerBuilder builder = new TaskSchedulerBuilder();
        builder = builder.poolSize(10);
        TaskSchedulingProperties.Shutdown shutdown = new TaskSchedulingProperties.Shutdown();
        builder = builder.awaitTermination(shutdown.isAwaitTermination());
        builder = builder.awaitTerminationPeriod(shutdown.getAwaitTerminationPeriod());
        builder = builder.threadNamePrefix("keray-task-scheduler");
        taskScheduler = builder.build();
        taskScheduler.initialize();
    }

    private static IContext getContext() {
        if (userContext == null) {
            synchronized (SysThreadPool.class) {
                try {
                    userContext = SpringContextHolder.getBean(IContext.class);
                } catch (Exception ignore) {
                    userContext = new IContext() {

                        @Override
                        public String currentUserId() {
                            return "";
                        }

                        @Override
                        public void setUserId(String userId) {

                        }

                        @Override
                        public String currentIp() {
                            return "";
                        }

                        @Override
                        public void setIp(String ip) {

                        }

                        @Override
                        public Map<String, Object> export() {
                            return null;
                        }
                    };
                }
            }
        }
        return userContext;
    }


    public static void execute(Runnable runnable) {
        execute(runnable, false);
    }

    public static void execute(Runnable runnable, boolean context) {
        if (context) {
            Map<String, Object> data = getContext().export();
            threadPoolExecutor.execute(() -> {
                userContext.importConf(data);
                runnable.run();
                userContext.clear();
            });
        } else {
            threadPoolExecutor.execute(runnable);
        }
    }


    public static Future<?> submit(Runnable runnable) {
        return submit(runnable, false);
    }

    public static Future<?> submit(Runnable runnable, boolean context) {
        if (context) {
            Map<String, Object> data = getContext().export();
            return threadPoolExecutor.submit(() -> {
                userContext.importConf(data);
                runnable.run();
                userContext.clear();
            });
        } else {
            return threadPoolExecutor.submit(runnable);
        }
    }


    public static <T> Future<T> submit(Callable<T> task) {
        return submit(task, false);
    }

    public static <T> Future<T> submit(Callable<T> task, boolean context) {
        if (context) {
            Map<String, Object> data = getContext().export();
            return threadPoolExecutor.submit(() -> {
                userContext.importConf(data);
                T t = task.call();
                userContext.clear();
                return t;
            });
        } else {
            return threadPoolExecutor.submit(task);
        }
    }

    public static void close() {
        threadPoolExecutor.shutdownNow();
        taskScheduler.shutdown();
    }

}
