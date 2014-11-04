package com.soundcloud.android.properties;

import com.soundcloud.android.R;

public enum Feature {

    TEST_FEATURE(-1),
    LEAVE_BEHIND(R.bool.feature_leave_behind),
    PAYMENTS(R.bool.feature_payments),
    API_MOBILE_SEARCH(R.bool.feature_api_mobile_search),
    HTTPCLIENT_REFACTOR(R.bool.feature_httpclient_refactor),
    LOCALYTICS_PUSH(R.bool.feature_localytics_push),
    OKHTTP(R.bool.feature_okhttp),
    INTERSTITIAL(R.bool.feature_interstitial),
    ADJUST_TRACKING(R.bool.feature_adjust),
    SECURE_STREAM_CACHE(R.bool.feature_secure_stream_cache);

    private final int resourceId;

    private Feature(int resourceId) {
        this.resourceId = resourceId;
    }

    public int getId() {
        return resourceId;
    }

}
