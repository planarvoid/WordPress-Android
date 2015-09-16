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

    public TestDateProvider(Date date) {
        timeInMillis = date.getTime();
    }

    @Override
    public Date getCurrentDate() {
        return new Date(timeInMillis);
    }

    @Override
    public long getCurrentTime() {
        return timeInMillis;
    }

    public void advanceBy(long time, TimeUnit timeUnit) {
        timeInMillis += timeUnit.toMillis(time);
    }

    public void setTime(long time, TimeUnit unit) {
        this.timeInMillis = unit.toMillis(time);
    }
}
