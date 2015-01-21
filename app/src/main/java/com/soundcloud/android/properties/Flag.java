package com.soundcloud.android.properties;

import com.soundcloud.android.R;

public enum Flag {

    TEST_FEATURE(-1),
    PAYMENTS(R.bool.feature_payments),
    PAYMENTS_TEST(R.bool.feature_payments_test),
    OKHTTP(R.bool.feature_okhttp),
    GOOGLE_CAST(R.bool.feature_google_cast),
    API_MOBILE_STREAM(R.bool.feature_api_mobile_stream),
    NEW_LIKES_END_TO_END(R.bool.feature_new_likes_end_to_end),
    CONFIGURATION_FEATURES(R.bool.feature_configuration_features),
    RELOAD_LAST_PLAYQUEUE(R.bool.feature_reload_last_playqueue);
    private final int resourceId;

    private Flag(int resourceId) {
        this.resourceId = resourceId;
    }

    public int getId() {
        return resourceId;
    }

}
