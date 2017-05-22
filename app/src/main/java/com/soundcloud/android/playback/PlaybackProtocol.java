package com.soundcloud.android.playback;

public enum PlaybackProtocol {
    HLS("hls"), ENCRYPTED_HLS("encrypted-hls"), FILE("file"), HTTPS("https"), UNKNOWN("unknown");

    private final String value;

    PlaybackProtocol(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static PlaybackProtocol fromValue(String value) {
        if (HLS.getValue().equals(value)) {
            return HLS;
        } else if (ENCRYPTED_HLS.getValue().equals(value)) {
            return ENCRYPTED_HLS;
        } else if (FILE.getValue().equals(value)) {
            return FILE;
        }
        return UNKNOWN;
    }
}
