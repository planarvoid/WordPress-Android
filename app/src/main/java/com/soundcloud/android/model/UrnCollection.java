package com.soundcloud.android.model;

import android.support.annotation.NonNull;

public enum UrnCollection {
    TRACKS("tracks"),
    USERS("users"),
    PLAYLISTS("playlists"),
    STATIONS("stations"),
    TRACK_STATIONS("track-stations"),
    ARTIST_STATIONS("artist-stations"),
    COMMENTS("comments"),
    DAY_ZERO("dayzero"),
    ADS("ads"),
    SOUNDS("sounds"),
    GENRES("genres"),
    NEW_FOR_YOU("newforyou"),
    SYSTEM_PLAYLIST("system-playlists"),
    UNKNOWN("unknown");

    private static final String STATIONS_REGEX = "[\\w-]+-stations";

    private String value;

    UrnCollection(String value) {
        this.value = value;
    }

    String value() {
        return value;
    }

    public static UrnCollection from(Urn urn) {
        return urn.getCollection();
    }

    public static UrnCollection from(@NonNull String part) {
        for (UrnCollection collection : UrnCollection.values()) {
            if (collection.value().equals(part)) {
                return collection;
            }
        }
        if (part.matches(STATIONS_REGEX)) {
            return UrnCollection.STATIONS;
        } else {
            return UrnCollection.UNKNOWN;
        }
    }

    @Override
    public String toString() {
        return value;
    }

}
