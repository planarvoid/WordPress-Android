package com.soundcloud.android.utils;

import android.os.PowerManager;

import javax.inject.Inject;

public class PowerManagerWakeLockWrapper {

    private final PowerManager.WakeLock wakeLock;

    @Inject
    public PowerManagerWakeLockWrapper(PowerManager.WakeLock wakeLock) {
        this.wakeLock = wakeLock;
    }

    public void acquire() {
        wakeLock.acquire();
    }

    public boolean isHeld() {
        return wakeLock.isHeld();
    }

    public void release() {
        wakeLock.release();
    }
}
