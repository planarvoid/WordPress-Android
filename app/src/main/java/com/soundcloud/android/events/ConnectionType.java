package com.soundcloud.android.events;

public enum ConnectionType {
    TWO_G("2g"),
    THREE_G("3g"),
    FOUR_G("4g"),
    WIFI("wifi"),
    OFFLINE("offline"),
    UNKNOWN("unknown");
    private final String value;

    ConnectionType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

}
