package me.eldodebug.soar.utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.AbortPolicy;

public class Multithreading {

    private static final int AVAILABLE_PROCESSORS = Math.max(1, Runtime.getRuntime().availableProcessors());
    private static final int SCHEDULED_THREADS = Math.min(2, AVAILABLE_PROCESSORS);
    private static final int CORE_ASYNC_THREADS = Math.min(2, AVAILABLE_PROCESSORS);
    private static final int MAX_ASYNC_THREADS = Math.max(2, Math.min(AVAILABLE_PROCESSORS, 4));

    private static final ThreadFactory SCHEDULED_THREAD_FACTORY = newThreadFactory("Flax-Scheduler");
    private static final ThreadFactory ASYNC_THREAD_FACTORY = newThreadFactory("Flax-Async");

    private static final ScheduledThreadPoolExecutor RUNNABLE_POOL = new ScheduledThreadPoolExecutor(SCHEDULED_THREADS, SCHEDULED_THREAD_FACTORY);

    public static final ExecutorService POOL = new ThreadPoolExecutor(CORE_ASYNC_THREADS, MAX_ASYNC_THREADS, 30L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<Runnable>(), ASYNC_THREAD_FACTORY, new AbortPolicy());

    static {
        RUNNABLE_POOL.setRemoveOnCancelPolicy(true);
        RUNNABLE_POOL.setKeepAliveTime(30L, TimeUnit.SECONDS);
        RUNNABLE_POOL.allowCoreThreadTimeOut(true);
    }

    public static void schedule(Runnable r, long initialDelay, long delay, TimeUnit unit) {
        RUNNABLE_POOL.scheduleAtFixedRate(r, initialDelay, delay, unit);
    }

    public static ScheduledFuture<?> schedule(Runnable r, long delay, TimeUnit unit) {
        return Multithreading.RUNNABLE_POOL.schedule(r, delay, unit);
    }

    public static int getTotal() {
        return ((ThreadPoolExecutor) Multithreading.POOL).getActiveCount();
    }

    public static void runAsync(Runnable runnable) {
        POOL.execute(runnable);
    }

    private static ThreadFactory newThreadFactory(final String namePrefix) {
        return new ThreadFactory() {
            private int threadId;

            @Override
            public synchronized Thread newThread(Runnable runnable) {
                Thread thread = new Thread(runnable, namePrefix + "-" + threadId++);
                thread.setDaemon(true);
                return thread;
            }
        };
    }
}
