package com.soundcloud.android.configuration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class DeviceManagement {

    private final boolean authorized;
    private final boolean recoverable;

    @JsonCreator
    public DeviceManagement(@JsonProperty("authorized") boolean authorized,
                            @JsonProperty("recoverable") boolean recoverable) {
        this.authorized = authorized;
        this.recoverable = recoverable;
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

}
