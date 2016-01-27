package com.soundcloud.android.model;

import android.support.annotation.NonNull;

public enum UrnNamespace {
    SOUNDCLOUD("soundcloud"),
    LOCAL("local"),
    OTHER("other");

    private String value;

    UrnNamespace(String value) {
        this.value = value;
    }

    String value() {
        return value;
    }

    static UrnNamespace from(@NonNull String part) {
        for (UrnNamespace namespace : UrnNamespace.values()) {
            if (namespace.value().equals(part)) {
                return namespace;
            }
        }
        return OTHER;
    }

    @Override
    public String toString() {
        return value;
    }
}
