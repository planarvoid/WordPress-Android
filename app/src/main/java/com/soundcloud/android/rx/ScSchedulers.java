package com.soundcloud.android.rx;

import rx.Scheduler;
import rx.concurrency.Schedulers;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

public final class ScSchedulers {

    public static final Scheduler STORAGE_SCHEDULER;
    public static final Scheduler API_SCHEDULER;

    private static final int NUM_API_THREADS = 3;

    static {
        STORAGE_SCHEDULER = Schedulers.threadPoolForIO();
        API_SCHEDULER = Schedulers.executor(createApiExecutor());
    }

    private static Executor createApiExecutor() {
        return Executors.newFixedThreadPool(NUM_API_THREADS, new ThreadFactory() {
            final AtomicLong counter = new AtomicLong();

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "RxApiThreadPool-" + counter.incrementAndGet());
                t.setDaemon(true);
                return t;
            }
        });
    }


}
