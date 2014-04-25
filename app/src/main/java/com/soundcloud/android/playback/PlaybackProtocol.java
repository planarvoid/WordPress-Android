package com.soundcloud.android.playback;

public enum PlaybackProtocol {
    HLS("hls"), HTTPS("https");

    private final String value;

    PlaybackProtocol(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
