package com.soundcloud.android.properties;

import com.soundcloud.android.R;

public enum Feature {

    TEST_FEATURE(-1),
    LEAVE_BEHIND(R.bool.feature_leave_behind),
    COMMENTS_REDESIGN(R.bool.feature_comments_redesign),
    PAYMENTS(R.bool.feature_payments),
    API_MOBILE_SEARCH(R.bool.feature_api_mobile_search),
    INTERSTITIAL(R.bool.feature_interstitial);

    private final int resourceId;

    private Feature(int resourceId) {
        this.resourceId = resourceId;
    }

    public int getId() {
        return resourceId;
    }

}
