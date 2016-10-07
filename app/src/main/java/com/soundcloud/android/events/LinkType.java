package com.soundcloud.android.events;

public enum LinkType {
    ATTRIBUTOR("attributor"),
    OWNER("owner"),
    SELF("self");

    private final String name;

    LinkType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
