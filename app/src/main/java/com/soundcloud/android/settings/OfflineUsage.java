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

    public long getOfflineTotal() {
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
        return Math.max(0, getOfflineTotal() - offlineUsed);
    }

    public long getAvailableWithoutOfflineLimit() {
        return deviceAvailable - getOfflineAvailable();
    }

    public int getOfflineTotalPercentage() {
        if (getAvailableBeforeOffline() == 0) {
            return 0;
        }

        return Math.round(Math.max(0, Math.min(100, offlineTotal * 100 / getAvailableBeforeOffline())));
    }

    public void setOfflineTotalPercentage(int percentage) {
        int numberOfSteps = Math.max(percentage / getIncrementStep(), 1);
        offlineTotal = (long) (numberOfSteps * STEP_IN_BYTES);
    }

    public int getIncrementStep() {
        return (int) Math.round((STEP_IN_BYTES / getAvailableBeforeOffline()) * 100d);
    }

    private long getAvailableBeforeOffline() {
        return deviceAvailable + offlineUsed;
    }

}
