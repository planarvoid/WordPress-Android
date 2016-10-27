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

    private boolean isUnlimited;

    @Inject
    OfflineUsage(SecureFileStorage fileStorage, OfflineSettingsStorage offlineSettings) {
        this.fileStorage = fileStorage;
        this.offlineSettings = offlineSettings;
    }

    public void update() {
        this.deviceTotal = fileStorage.getStorageCapacity();
        this.deviceAvailable = fileStorage.getStorageAvailable();
        this.offlineLimit = offlineSettings.getStorageLimit();
        this.offlineUsed = fileStorage.getStorageUsed();
        this.isUnlimited = !offlineSettings.hasStorageLimit();
    }

    long getUsableOfflineLimit() {
        return isUnlimited
               ? getUnlimitedSize()
               : Math.min(offlineLimit, getUnlimitedSize());
    }

    long getActualOfflineLimit() {
        return isUnlimited
               ? getUnlimitedSize()
               : offlineLimit;
    }

    private long getUnlimitedSize() {
        return deviceAvailable + offlineUsed;
    }

    long getOfflineUsed() {
        return offlineUsed;
    }

    long getDeviceAvailable() {
        return deviceAvailable;
    }

    long getDeviceTotal() {
        return deviceTotal;
    }

    long getUsedOthers() {
        return deviceTotal - deviceAvailable - offlineUsed;
    }

    long getOfflineAvailable() {
        return Math.max(0, getUsableOfflineLimit() - offlineUsed);
    }

    public long getUnused() {
        return deviceAvailable - getOfflineAvailable();
    }

    int getOfflineLimitPercentage() {
        if (isUnlimited || deviceTotal == 0) {
            return 100;
        } else {
            return (int) (offlineLimit * 100 / deviceTotal);
        }
    }

    boolean setOfflineLimitPercentage(int percentage) {
        int step = (int) Math.max(Math.ceil(percentage / getStepPercentage()), 1);
        long calculatedLimit = Math.min((long) (step * STEP_IN_BYTES), deviceTotal);

        isUnlimited = percentage >= 100 - getStepPercentage();

        if (calculatedLimit < offlineUsed) {
            offlineLimit = offlineUsed;
            return false;
        }

        offlineLimit = calculatedLimit;
        return true;
    }

    private double getStepPercentage() {
        return STEP_IN_BYTES / deviceTotal * 100;
    }

    public boolean isUnlimited() {
        return isUnlimited;
    }

}
