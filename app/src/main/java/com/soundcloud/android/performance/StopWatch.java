package com.soundcloud.android.performance;

import java.util.concurrent.TimeUnit;

public class StopWatch {
    private long startTime;
    private long endTime;

    public StopWatch() {
        startTime = System.nanoTime();
        endTime = 0;
    }

    void stop() {
        endTime = System.nanoTime();
    }

    long getTotalTimeMillis() {
        return TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
    }
}
