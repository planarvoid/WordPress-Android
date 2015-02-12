package com.soundcloud.android.properties;

import com.soundcloud.android.R;

public enum Flag {

    TEST_FEATURE(-1),
    PAYMENTS(R.bool.feature_payments),
    PAYMENTS_TEST(R.bool.feature_payments_test),
    GOOGLE_CAST(R.bool.feature_google_cast),
    API_MOBILE_STREAM(R.bool.feature_api_mobile_stream),
    CONFIGURATION_FEATURES(R.bool.feature_configuration_features),
    RELOAD_LAST_PLAYQUEUE(R.bool.feature_reload_last_playqueue),
    PLAYLIST_POSTS_FRAGMENT(R.bool.feature_playlist_posts_fragment);

    private final int resourceId;

    private Flag(int resourceId) {
        this.resourceId = resourceId;
    }

    public int getId() {
        return resourceId;
    }

}
