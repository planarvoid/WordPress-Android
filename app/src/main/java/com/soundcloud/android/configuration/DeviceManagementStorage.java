package com.soundcloud.android.configuration;

import com.google.common.annotations.VisibleForTesting;

import android.content.SharedPreferences;

import javax.inject.Inject;
import javax.inject.Named;

public class DeviceManagementStorage {

    @VisibleForTesting
    static final String DEVICE_CONFLICT = "device_conflict";

    private final SharedPreferences sharedPreferences;

    @Inject
    public DeviceManagementStorage(@Named("DeviceManagement") SharedPreferences sharedPreferences) {
        this.sharedPreferences = sharedPreferences;
    }

    public void setDeviceConflict() {
        sharedPreferences.edit().putBoolean(DEVICE_CONFLICT, true).apply();
    }

    public void clearDeviceConflict() {
        sharedPreferences.edit().putBoolean(DEVICE_CONFLICT, false).apply();
    }

    public boolean hadDeviceConflict() {
        return sharedPreferences.getBoolean(DEVICE_CONFLICT, false);
    }

}
