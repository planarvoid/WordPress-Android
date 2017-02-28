package com.soundcloud.android.analytics.performance;

import java.util.concurrent.TimeUnit;

public class StopWatch {
    private long startTime;
    private long endTime;

    private StopWatch() {
        startTime = System.nanoTime();
        endTime = 0;
    }

    public static StopWatch start() {
        return new StopWatch();
    }

    void stop() {
        endTime = System.nanoTime();
    }

    long getTotalTimeMillis() {
        return TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
    }
}
