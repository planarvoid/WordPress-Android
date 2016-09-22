package com.soundcloud.android.suggestedcreators;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.soundcloud.java.strings.Strings;

public enum SuggestedCreatorRelation {
    LISTENED_TO("listened_to"),
    LIKED("liked"),
    CURATED("curated"),
    REPOSTED("reposted"),
    UPLOADED("uploaded"),
    UNKNOWN("unknown");

    private final String value;

    SuggestedCreatorRelation(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static SuggestedCreatorRelation from(String str) {
        if (!Strings.isBlank(str)) {
            for (SuggestedCreatorRelation s : values()) {
                if (s.value.equalsIgnoreCase(str)) {
                    return s;
                }
            }
        }
        return UNKNOWN;
    }
}
