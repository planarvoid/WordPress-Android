package com.soundcloud.android.framework.helpers.networkmanager;

import android.os.SystemClock;

import java.util.concurrent.TimeUnit;

public class Waiter {
    private static final long TIMEOUT = TimeUnit.SECONDS.toMillis(10);
    private static final int SLEEPING_TIME = 1000;

    public boolean waitForServiceBinding(NetworkManager networkManager) {
        for (long endTime = now() + TIMEOUT ; !networkManager.isBound() && now() < endTime;) {
            SystemClock.sleep(SLEEPING_TIME);
        }
        return networkManager.isBound();
    }

    private long now() {
        return System.currentTimeMillis();
    }

    public boolean waitForMessage(ResponseHandler responseHandler, int id) {
        final long endTime = now() + TIMEOUT;

        do {
            if(now() > endTime) {
                return false;
            }
            SystemClock.sleep(SLEEPING_TIME);
        } while(!responseHandler.hasMessage(id));
        return responseHandler.hasMessage(id);
    }
}
