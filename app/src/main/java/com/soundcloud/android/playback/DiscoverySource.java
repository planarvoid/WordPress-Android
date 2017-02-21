package com.soundcloud.android.playback;

import static java.util.Locale.US;

public enum DiscoverySource {
    RECOMMENDER("recommender"),
    STATIONS("stations"),
    STREAM("stream"),
    STATIONS_SUGGESTIONS("stations:suggestions"),
    HISTORY("history"),
    RECENTLY_PLAYED("recently_played"),
    PLAY_NEXT("play_next"),
    RECOMMENDATIONS("personal-recommended"),
    CAST("cast"),
    NEW_FOR_YOU("new_for_you");

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
