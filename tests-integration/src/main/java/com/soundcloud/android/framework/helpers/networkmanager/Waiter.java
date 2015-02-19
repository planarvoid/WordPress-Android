package com.soundcloud.android.framework.helpers.networkmanager;

import android.os.SystemClock;

import java.util.concurrent.TimeUnit;

public class Waiter {
    private static final long TIMEOUT = TimeUnit.SECONDS.toMillis(10);

    public boolean waitForMessage(ResponseHandler responseHandler, int id) {
        long endTime = SystemClock.uptimeMillis() + TIMEOUT;

        do {
            boolean timedOut = SystemClock.uptimeMillis() > endTime;
            if(timedOut) {
                return false;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } while(!responseHandler.hasMessage(id));
        return responseHandler.hasMessage(id);
    }
}
