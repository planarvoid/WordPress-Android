package com.soundcloud.android.utils;

import java.util.Date;

public class DateProviderStub extends DateProvider {
    private final Date date;

    public DateProviderStub(long timeInMillis) {
        this.date = new Date(timeInMillis);
    }

    public DateProviderStub(Date date) {
        this.date = date;
    }

    public DateProviderStub() {
        date = new Date();
    }

    @Override
    public Date getCurrentDate() {
        return date;
    }

    @Override
    public long getCurrentTime() {
        return date.getTime();
    }
}
