package com.soundcloud.android.settings;

import com.soundcloud.android.offline.OfflineSettingsStorage;
import com.soundcloud.android.offline.SecureFileStorage;

import javax.inject.Inject;

class OfflineUsage {
    private final double STEP_IN_BYTES = 512 * 1024 * 1024;

    private final SecureFileStorage fileStorage;
    private final OfflineSettingsStorage offlineSettings;

    private long deviceTotal;
    private long deviceAvailable;
    private long offlineLimit;
    private long offlineUsed;

    @Inject
    public OfflineUsage(SecureFileStorage fileStorage, OfflineSettingsStorage offlineSettings) {
        this.fileStorage = fileStorage;
        this.offlineSettings = offlineSettings;
    }

    public void update() {
        this.deviceTotal = fileStorage.getStorageCapacity();
        this.deviceAvailable = fileStorage.getStorageAvailable();
        this.offlineLimit = offlineSettings.getStorageLimit();
        this.offlineUsed = fileStorage.getStorageUsed();
    }

    public long getOfflineLimit() {
        return Math.min(offlineLimit, deviceAvailable + offlineUsed);
    }

    public long getOfflineUsed() {
        return offlineUsed;
    }

    public long getDeviceAvailable() {
        return deviceAvailable;
    }

    public long getDeviceTotal() {
        return deviceTotal;
    }

    public long getUsedOthers() {
        return deviceTotal - deviceAvailable - offlineUsed;
    }

    public long getOfflineAvailable() {
        return Math.max(0, getOfflineLimit() - offlineUsed);
    }

    public long getUnused() {
        return deviceAvailable - getOfflineAvailable();
    }

    public int getOfflineLimitPercentage() {
        return (int) (offlineLimit * 100 / deviceTotal);
    }

    public boolean setOfflineLimitPercentage(int percentage) {
        int steps = (int) Math.max(Math.ceil(percentage / getStepPercentage()), 1);
        long calculatedLimit = Math.min((long) (steps * STEP_IN_BYTES), deviceTotal);

        if (calculatedLimit < offlineUsed) {
            offlineLimit = offlineUsed;
            return false;
        }

        offlineLimit = calculatedLimit;
        return true;
    }

    public double getStepPercentage() {
        return STEP_IN_BYTES / deviceTotal * 100;
    }

    public boolean isMaximumLimit() {
        return offlineLimit > (deviceTotal - STEP_IN_BYTES);
    }

}
