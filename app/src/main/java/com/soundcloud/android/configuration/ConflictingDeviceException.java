package com.soundcloud.android.configuration;

public class ConflictingDeviceException extends Exception {

    public final String deviceId;

    public ConflictingDeviceException(String deviceId) {
        this.deviceId = deviceId;
    }

}
