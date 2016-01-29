package com.soundcloud.android.configuration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class DeviceManagement {

    private final boolean authorized;
    private final boolean recoverable;
    private final String conflictingDeviceId;

    @JsonCreator
    public DeviceManagement(@JsonProperty("authorized") boolean authorized,
                            @JsonProperty("recoverable") boolean recoverable,
                            @JsonProperty("device_id") String conflictingDeviceId) {
        this.authorized = authorized;
        this.recoverable = recoverable;
        this.conflictingDeviceId = conflictingDeviceId;
    }

    public boolean isUnauthorized() {
        return !authorized;
    }

    public boolean isRecoverableBlock() {
        return !authorized && recoverable;
    }

    public boolean isUnrecoverableBlock() {
        return !authorized && !recoverable;
    }

    public String getConflictingDeviceId() {
        return conflictingDeviceId;
    }

}
