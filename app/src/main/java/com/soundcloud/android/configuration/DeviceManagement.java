package com.soundcloud.android.configuration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class DeviceManagement {

    private final boolean authorized;
    private final String conflictingDeviceId;

    @JsonCreator
    public DeviceManagement(@JsonProperty("authorized") boolean authorized,
                            @JsonProperty("device_id") String conflictingDeviceId) {
        this.authorized = authorized;
        this.conflictingDeviceId = conflictingDeviceId;
    }

    public boolean isNotAuthorized() {
        return !authorized;
    }

    public String getConflictingDeviceId() {
        return conflictingDeviceId;
    }
}
