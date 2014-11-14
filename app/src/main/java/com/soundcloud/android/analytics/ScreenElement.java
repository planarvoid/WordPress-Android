package com.soundcloud.android.analytics;

public enum ScreenElement {

    PLAYER("player"),
    LIST("list");

    private final String trackingTag;

    private ScreenElement(String trackingTag) {
        this.trackingTag = trackingTag;
    }

    public String get() {
        return trackingTag;
    }

}
