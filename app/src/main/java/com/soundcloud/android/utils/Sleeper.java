package com.soundcloud.android.utils;

import android.os.SystemClock;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.TimeUnit;

@Singleton
public class Sleeper {

    @Inject
    public Sleeper() {
    }

    public void sleep(long sleepTime, TimeUnit timeUnit) {
        sleep(timeUnit.toMillis(sleepTime));
    }

    public void sleep(long sleepTime) {
        SystemClock.sleep(sleepTime);
    }
}
