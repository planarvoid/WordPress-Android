package com.soundcloud.android.properties;

import com.soundcloud.android.R;

public enum Feature {

    NEW_STREAM(R.bool.feature_new_stream),
    VISUAL_PLAYER(R.bool.feature_visual_player);

    private final int resourceId;

    private Feature(int resourceId) {
        this.resourceId = resourceId;
    }

    public int getId() {
        return resourceId;
    }

}
