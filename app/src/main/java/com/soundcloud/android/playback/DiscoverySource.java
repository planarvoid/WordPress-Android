package com.soundcloud.android.playback;

import static java.util.Locale.US;

public enum DiscoverySource {
    RECOMMENDER("recommender"),
    EXPLORE("explore"),
    STATIONS("stations"),
    STREAM("stream"),
    STATIONS_SUGGESTIONS("stations:suggestions");

    private String value;

    DiscoverySource(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static DiscoverySource from(String value) {
        return DiscoverySource.valueOf(value.toUpperCase(US).replaceAll(":", "_"));
    }
}
