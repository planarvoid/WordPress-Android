package com.soundcloud.android.playback;

import java.util.Locale;

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
    NEW_FOR_YOU("new_for_you"),
    DEEPLINK("deeplink"); // TODO (REC-1302): Check whether that is correct

    private String value;

    DiscoverySource(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static DiscoverySource from(String value) {
        for (DiscoverySource source : values()) {
            if (source.value.equalsIgnoreCase(value)) {
                return source;
            }
        }
        return DiscoverySource.valueOf(value.toUpperCase(Locale.US).replaceAll(":", "_"));
    }
}
