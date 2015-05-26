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
    private long offlineTotal;
    private long offlineUsed;

    @Inject
    public OfflineUsage(SecureFileStorage fileStorage, OfflineSettingsStorage offlineSettings) {
        this.fileStorage = fileStorage;
        this.offlineSettings = offlineSettings;
    }

    public void update() {
        this.deviceTotal = fileStorage.getStorageCapacity();
        this.deviceAvailable = fileStorage.getStorageAvailable();
        this.offlineTotal = offlineSettings.getStorageLimit();
        this.offlineUsed = fileStorage.getStorageUsed();
    }

    public long getOfflineLimit() {
        return Math.min(offlineTotal, deviceAvailable + offlineUsed);
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
        return (int) (offlineTotal * 100 / deviceTotal);
    }

    public void setOfflineLimitPercentage(int percentage) {
        int steps = (int) Math.max(Math.ceil(percentage / getStepPercentage()), 1);
        offlineTotal = Math.min((long) (steps * STEP_IN_BYTES), deviceTotal);
    }

    public double getStepPercentage() {
        return STEP_IN_BYTES / deviceTotal * 100;
    }

    public boolean isMaximumLimit() {
        return offlineTotal > (deviceTotal - STEP_IN_BYTES);
    }

}
