package com.soundcloud.android.rx;

import org.jetbrains.annotations.NotNull;
import rx.Scheduler;
import rx.schedulers.Schedulers;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

public final class ScSchedulers {

    public static final Scheduler STORAGE_SCHEDULER;
    public static final Scheduler API_SCHEDULER;
    public static final Scheduler GRAPHICS_SCHEDULER;
    public static final Scheduler SEQUENTIAL_SYNCING_SCHEDULER;

    static {
        STORAGE_SCHEDULER = Schedulers.from(createExecutor("RxStorageThreadPool", 3));
        API_SCHEDULER = Schedulers.from(createExecutor("RxApiThreadPool", 3));
        GRAPHICS_SCHEDULER= Schedulers.from(createExecutor("RxGraphicsThreadPool", 1));
        SEQUENTIAL_SYNCING_SCHEDULER = Schedulers.from(createExecutor("RxSequentialDownloadThreadPool", 1));
    }

    private static Executor createExecutor(final String threadIdentifier, int numThreads) {
        return Executors.newFixedThreadPool(numThreads, new ThreadFactory() {
            final AtomicLong counter = new AtomicLong();

            @Override
            public Thread newThread(@NotNull Runnable r) {
                Thread t = new Thread(r, threadIdentifier + "-" + counter.incrementAndGet());
                t.setDaemon(true);
                return t;
            }
        });
    }


}
