package com.soundcloud.android.rx;

import rx.Scheduler;
import rx.concurrency.Schedulers;

import java.util.concurrent.Executors;

public final class ScSchedulers {

    public static final Scheduler STORAGE_SCHEDULER;
    public static final Scheduler API_SCHEDULER;

    private static final int NUM_API_THREADS = 3;

    static {
        STORAGE_SCHEDULER = Schedulers.threadPoolForIO();
        //TODO: could we use threadPoolForIO here as well?
        API_SCHEDULER = Schedulers.executor(Executors.newFixedThreadPool(NUM_API_THREADS));
    }



}
