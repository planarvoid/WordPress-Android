package com.soundcloud.android.properties;

import com.soundcloud.android.R;

public enum Feature {

    TEST_FEATURE(-1),
    PAYMENTS(R.bool.feature_payments),
    PAYMENTS_TEST(R.bool.feature_payments_test),
    LOCALYTICS_PUSH(R.bool.feature_localytics_push),
    OKHTTP(R.bool.feature_okhttp),
    SECURE_STREAM_CACHE(R.bool.feature_secure_stream_cache),
    API_MOBILE_STREAM(R.bool.feature_api_mobile_stream),
    TRACK_ITEM_OVERFLOW(R.bool.feature_track_item_overflow),
    APP_KEY_GENERATION(R.bool.feature_app_key_generation);

    private final int resourceId;

    private Feature(int resourceId) {
        this.resourceId = resourceId;
    }

    public int getId() {
        return resourceId;
    }

}
