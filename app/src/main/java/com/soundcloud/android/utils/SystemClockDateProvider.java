package com.soundcloud.android.utils;

import android.os.SystemClock;

import javax.inject.Inject;
import java.util.Date;

public class SystemClockDateProvider implements DateProvider {

    @Inject
    public SystemClockDateProvider() {
    }

    @Override
    public Date getDate() {
        return new Date(SystemClock.uptimeMillis());
    }

    @Override
    public long getTime() {
        return SystemClock.uptimeMillis();
    }
}
