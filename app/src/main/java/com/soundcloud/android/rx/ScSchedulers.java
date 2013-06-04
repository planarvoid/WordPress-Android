package com.soundcloud.android.rx;

import com.soundcloud.android.rx.schedulers.MainThreadScheduler;
import rx.Scheduler;
import rx.concurrency.Schedulers;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ScSchedulers {

    public static final Scheduler BACKGROUND_SCHEDULER;
    public static final Scheduler UI_SCHEDULER;

    private static final ExecutorService sExecutor;

    static {
        //TODO: was a bit cautious here, should look into using a thread pool once things stabilize and our
        //concurrency patterns emerge visibly
        sExecutor = Executors.newSingleThreadExecutor();

        BACKGROUND_SCHEDULER = Schedulers.executor(sExecutor);
        UI_SCHEDULER = new MainThreadScheduler();
    }



}
