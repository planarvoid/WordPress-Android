package com.soundcloud.android.events;

public enum PlayerType {
    SKIPPY("Skippy"), MEDIA_PLAYER("MediaPlayer");

    private final String value;

    PlayerType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
