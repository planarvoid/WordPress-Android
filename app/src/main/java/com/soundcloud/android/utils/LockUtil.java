package com.soundcloud.android.utils;

import javax.inject.Inject;

public class LockUtil {

    public static final String WAKE_LOCK_TAG = "LockUtilWakeLock";
    private final PowerManagerWakeLockWrapper wakeLock;

    @Inject
    public LockUtil(PowerManagerWrapper powerManager) {
        wakeLock = powerManager.newPartialWakeLock(WAKE_LOCK_TAG);
    }

    public void lock() {
        Log.d(this, "WakeLockUtil.lock()");

        if (!wakeLock.isHeld()){
            try {
                wakeLock.acquire();
            } catch (Exception e) {
                Log.e(this, "Error getting Wake Lock: " + e.getMessage());
            }
        }
    }

    public void unlock() {
        Log.d(this, "WakeLockUtil.unlock()");
        if (wakeLock.isHeld()){
            wakeLock.release();
        }
    }
}
