package com.soundcloud.android.events;

public enum PlayerType {
    SKIPPY("Skippy"), MEDIA_PLAYER("MediaPlayer"), VIDEO_MEDIA_PLAYER("VideoMediaPlayer");

    private final String value;

    PlayerType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
