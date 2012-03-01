package com.soundcloud.android.model;

public enum Plan {
    FREE(0, "free"),
    SOLO(1, "solo"),
    PRO(2, "pro"),
    PRO_PLUS(4, "pro plus"),
    LITE(8, "lite"),

    UNKNOWN(-1, "unknown");

    public final int id;
    public final String name;

    Plan(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public static Plan fromApi(String name) {
        if (name == null) return UNKNOWN;
        for (Plan p : values()) {
            if (p.name.equalsIgnoreCase(name)) return p;
        }
        return UNKNOWN;
    }
}
