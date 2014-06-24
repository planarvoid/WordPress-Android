package com.soundcloud.android.properties;

import com.soundcloud.android.R;

public enum Feature {

    VISUAL_PLAYER(R.bool.feature_visual_player),
    EMAIL_OPT_IN(R.bool.feature_email_opt_in);

    private final int resourceId;

    private Feature(int resourceId) {
        this.resourceId = resourceId;
    }

    public int getId() {
        return resourceId;
    }

}
