package com.soundcloud.android.utils;

import java.util.Date;
import java.util.concurrent.TimeUnit;

public class TestDateProvider extends CurrentDateProvider {
    private long timeInMillis;

    public TestDateProvider(long timeInMillis) {
        this.timeInMillis = timeInMillis;
    }

    public TestDateProvider() {
        timeInMillis = System.currentTimeMillis();
    }

    @Override
    public Date getDate() {
        return new Date(timeInMillis);
    }

    @Override
    public long getTime() {
        return timeInMillis;
    }

    public void advanceBy(long time, TimeUnit timeUnit) {
        timeInMillis += timeUnit.toMillis(time);
    }
}
