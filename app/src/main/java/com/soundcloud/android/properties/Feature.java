package com.soundcloud.android.properties;

import com.soundcloud.android.R;

public enum Feature {

    TEST_FEATURE(-1),
    PAYMENTS(R.bool.feature_payments),
    PAYMENTS_TEST(R.bool.feature_payments_test),
    OKHTTP(R.bool.feature_okhttp),
    GOOGLE_CAST(R.bool.feature_google_cast),
    API_MOBILE_STREAM(R.bool.feature_api_mobile_stream),
    OFFLINE_SYNC_FROM_LIKES(R.bool.feature_offline_sync_from_likes),
    TRACK_LIKES_SCREEN(R.bool.feature_track_likes_screen),
    NEW_LIKES_SYNCER(R.bool.feature_new_likes_syncer),
    PLAYLIST_LIKES_SCREEN(R.bool.feature_playlist_likes_screen);

    private final int resourceId;

    private Feature(int resourceId) {
        this.resourceId = resourceId;
    }

    public int getId() {
        return resourceId;
    }

}
