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
        SystemClock.sleep(timeUnit.toMillis(sleepTime));
    }

}
