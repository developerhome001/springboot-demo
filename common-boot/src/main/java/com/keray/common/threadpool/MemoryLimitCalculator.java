package com.keray.common.threadpool;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * {@link java.lang.Runtime#freeMemory()} technology is used to calculate the
 * memory limit by using the percentage of the current maximum available memory,
 */
public class MemoryLimitCalculator {

    private static volatile long maxAvailable;

    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor();

    static {
        // immediately refresh when this class is loaded to prevent maxAvailable from being 0
        refresh();
        // check every 50 ms to improve performance
        SCHEDULER.scheduleWithFixedDelay(MemoryLimitCalculator::refresh, 50, 50, TimeUnit.MILLISECONDS);
        Runtime.getRuntime().addShutdownHook(new Thread(SCHEDULER::shutdown));
    }

    private static void refresh() {
        var maxMemory = Runtime.getRuntime().maxMemory();
        var freeMemory = Runtime.getRuntime().freeMemory();
        var totalMemory = Runtime.getRuntime().totalMemory();
        var useMemory = totalMemory - freeMemory;
        maxAvailable = maxMemory - useMemory;
    }

    /**
     * Get the maximum available memory of the current JVM.
     *
     * @return maximum available memory
     */
    public static long maxAvailable() {
        return maxAvailable;
    }

    /**
     * Take the current JVM's maximum available memory
     * as a percentage of the result as the limit.
     *
     * @param percentage percentage
     * @return available memory
     */
    public static long calculate(final float percentage) {
        if (percentage <= 0 || percentage > 1) {
            throw new IllegalArgumentException();
        }
        return (long) (maxAvailable() * percentage);
    }

    /**
     * By default, it takes 80% of the maximum available memory of the current JVM.
     *
     * @return available memory
     */
    public static long defaultLimit() {
        return (long) (maxAvailable() * 0.8);
    }
}
