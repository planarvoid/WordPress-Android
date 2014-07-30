package com.soundcloud.android.properties;

public enum Feature {

    TEST_FEATURE(-1);

    private final int resourceId;

    private Feature(int resourceId) {
        this.resourceId = resourceId;
    }

    public int getId() {
        return resourceId;
    }

}
