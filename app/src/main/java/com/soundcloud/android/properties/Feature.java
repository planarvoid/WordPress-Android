package com.soundcloud.android.properties;

import com.soundcloud.android.R;

public enum Feature {

    TEST_FEATURE(-1),
    PAYMENTS(R.bool.feature_payments),
    API_MOBILE_SEARCH(R.bool.feature_api_mobile_search),
    LOCALYTICS_PUSH(R.bool.feature_localytics_push),
    OKHTTP(R.bool.feature_okhttp),
    INTERSTITIAL(R.bool.feature_interstitial),
    ADJUST_TRACKING(R.bool.feature_adjust),
    SECURE_STREAM_CACHE(R.bool.feature_secure_stream_cache),
    API_MOBILE_STREAM(R.bool.feature_api_mobile_stream),
    TRACK_ITEM_OVERFLOW(R.bool.feature_track_item_overflow);

    private final int resourceId;

    private Feature(int resourceId) {
        this.resourceId = resourceId;
    }

    public int getId() {
        return resourceId;
    }

}
