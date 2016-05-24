package com.soundcloud.android.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.soundcloud.java.strings.Strings;

public enum ChartType {
    TRENDING("trending"),
    TOP("top"),
    NONE("");

    private final String value;

    ChartType(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static ChartType from(String str) {
        if (!Strings.isBlank(str)) {
            for (ChartType s : values()) {
                if (s.value.equalsIgnoreCase(str)) {
                    return s;
                }
            }
        }
        return NONE;
    }
}
