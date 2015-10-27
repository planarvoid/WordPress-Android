package com.soundcloud.android.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.soundcloud.java.strings.Strings;

public enum Sharing {
    UNDEFINED(""),
    PUBLIC("public"),
    PRIVATE("private");

    public final String value;

    Sharing(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    // don't use built in valueOf to create so we can handle nulls and unknowns ourself
    @JsonCreator
    public static Sharing from(String str) {
        if (!Strings.isBlank(str)) {
            for (Sharing s : values()) {
                if (s.value.equalsIgnoreCase(str)) {
                    return s;
                }
            }
        }
        return UNDEFINED;
    }

    public static Sharing from(boolean isPublic) {
        return isPublic ? Sharing.PUBLIC : Sharing.PRIVATE;
    }

    public boolean isPublic() {
        return PUBLIC == this;
    }

    public boolean isPrivate() {
        return PRIVATE == this;
    }
}
