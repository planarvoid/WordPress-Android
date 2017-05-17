package com.soundcloud.android.image;

import com.fasterxml.jackson.annotation.JsonCreator;

import android.support.annotation.Nullable;

public enum ImageStyle {
    CIRCULAR("circular"),
    SQUARE("square"),
    STATION("station");

    private final String identifier;

    ImageStyle(String identifier) {
        this.identifier = identifier;
    }

    @JsonCreator
    @Nullable
    public static ImageStyle fromIdentifier(@Nullable String identifier) {
        if (identifier != null) {
            for (ImageStyle value : values()) {
                if (value.identifier.equals(identifier)) {
                    return value;
                }
            }
        }
        return null;
    }

    public String toIdentifier() {
        return identifier;
    }
}
