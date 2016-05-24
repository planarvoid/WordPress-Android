package com.soundcloud.android.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.soundcloud.java.strings.Strings;

public enum ChartCategory {
    AUDIO("audio"),
    MUSIC("music"),
    NONE("none");

    private final String value;

    ChartCategory(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static ChartCategory from(String str) {
        if (!Strings.isBlank(str)) {
            for (ChartCategory s : values()) {
                if (s.value.equalsIgnoreCase(str)) {
                    return s;
                }
            }
        }
        return NONE;
    }
}
