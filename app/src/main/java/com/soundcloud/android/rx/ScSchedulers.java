package com.soundcloud.android.rx;

import org.jetbrains.annotations.NotNull;
import rx.Scheduler;
import rx.schedulers.Schedulers;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

public final class ScSchedulers {

    public static final Scheduler HIGH_PRIO_SCHEDULER;
    public static final Scheduler LOW_PRIO_SCHEDULER;

    static {
        HIGH_PRIO_SCHEDULER = Schedulers.from(createExecutor("HighPriorityPool", 6));
        LOW_PRIO_SCHEDULER = Schedulers.from(createExecutor("LowPriorityPool", 1));
    }

    private static Executor createExecutor(final String threadIdentifier, int numThreads) {
        return Executors.newFixedThreadPool(numThreads, new ThreadFactory() {
            final AtomicLong counter = new AtomicLong();

            @Override
            public Thread newThread(@NotNull Runnable r) {
                return new Thread(r, threadIdentifier + "-" + counter.incrementAndGet());
            }
        });
    }


}
