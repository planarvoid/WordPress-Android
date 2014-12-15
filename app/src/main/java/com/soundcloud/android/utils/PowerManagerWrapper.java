package com.soundcloud.android.utils;

import android.os.PowerManager;

import javax.inject.Inject;

public class PowerManagerWrapper {

    private final PowerManager powerManager;

    @Inject
    public PowerManagerWrapper(PowerManager powerManager) {
        this.powerManager = powerManager;
    }

    public PowerManagerWakeLockWrapper newPartialWakeLock(String tag) {
        return newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, tag);
    }

    private PowerManagerWakeLockWrapper newWakeLock(int levelAndFlags, String tag) {
        return new PowerManagerWakeLockWrapper(powerManager.newWakeLock(levelAndFlags, tag));
    }
}
