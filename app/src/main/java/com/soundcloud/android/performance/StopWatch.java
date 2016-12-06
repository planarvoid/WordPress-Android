package com.soundcloud.android.performance;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

public class StopWatch {
    private long startTime;
    private long endTime;
    private long elapsedTime;

    @Inject
    public StopWatch() {
    }

    public void reset() {
        startTime = 0;
        endTime = 0;
        elapsedTime = 0;
    }

    public void start() {
        reset();
        startTime = System.nanoTime();
    }

    public void stop() {
        if (startTime != 0) {
            endTime = System.nanoTime();
            elapsedTime = endTime - startTime;
        } else {
            reset();
        }
    }

    public long getTotalTimeMillis() {
        return (elapsedTime != 0) ? TimeUnit.NANOSECONDS.toMillis(endTime - startTime) : 0;
    }
}
