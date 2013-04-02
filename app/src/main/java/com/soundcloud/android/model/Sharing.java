package com.soundcloud.android.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import android.text.TextUtils;

public enum Sharing {
    UNDEFINED(""),
    PUBLIC("public"),
    PRIVATE("private");

    final String value;

    Sharing(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    // don't use built in valueOf to create so we can handle nulls and unknowns ourself
    @JsonCreator
    public static Sharing fromString(String str) {
        if (!TextUtils.isEmpty(str)) {
            for (Sharing s : values()) {
                if (s.value.equalsIgnoreCase(str)) return s;
            }
        }
        return UNDEFINED;
    }

    public boolean isPublic() { return PUBLIC == this; }

    public boolean isPrivate() { return PRIVATE == this; }
}
