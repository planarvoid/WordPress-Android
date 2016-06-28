package com.soundcloud.android.playback;

public enum DiscoverySource {
    RECOMMENDER("recommender"),
    EXPLORE("explore"),
    STATIONS("stations"),
    STREAM("stream"),
    STATIONS_SUGGESTIONS("stations:suggestions");

    private String value;

    DiscoverySource(String value){
        this.value = value;
    }

    public String value() {
        return value;
    }
}
