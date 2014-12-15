package com.soundcloud.android.utils;

import android.net.wifi.WifiManager;

import javax.inject.Inject;

public class LockUtil {

    public static final String WIFI_LOCK_TAG = "LockUtilWifiLock";
    public static final String WAKE_LOCK_TAG = "LockUtilWakeLock";
    private final WifiManager.WifiLock wifiLock;
    private final PowerManagerWakeLockWrapper wakeLock;

    @Inject
    public LockUtil(WifiManager wifiManager, PowerManagerWrapper powerManager) {
        wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL, WIFI_LOCK_TAG);
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

        if (!wifiLock.isHeld()){
            try {
                wifiLock.acquire();
            } catch (Exception e) {
                Log.e(this, "Error getting Wifi Lock: " + e.getMessage());
            }
        }
    }

    public void unlock() {
        Log.d(this, "WakeLockUtil.unlock()");
        if (wakeLock.isHeld()){
            wakeLock.release();
        }

        if (wifiLock.isHeld()) {
            wifiLock.release();
        }
    }
}
