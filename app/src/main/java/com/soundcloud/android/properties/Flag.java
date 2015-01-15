package com.soundcloud.android.properties;

import com.soundcloud.android.R;

public enum Flag {

    TEST_FEATURE(-1),
    PAYMENTS(R.bool.feature_payments),
    PAYMENTS_TEST(R.bool.feature_payments_test),
    OKHTTP(R.bool.feature_okhttp),
    GOOGLE_CAST(R.bool.feature_google_cast),
    API_MOBILE_STREAM(R.bool.feature_api_mobile_stream),
    TRACK_LIKES_SCREEN(R.bool.feature_track_likes_screen),
    NEW_LIKES_SYNCER(R.bool.feature_new_likes_syncer),
    PLAYLIST_LIKES_SCREEN(R.bool.feature_playlist_likes_screen),
    CONFIGURATION_FEATURES(R.bool.feature_configuration_features),
    OFFLINE_SYNC_FROM_LIKES(R.bool.feature_offline_sync_from_likes),
    RELOAD_LAST_PLAYQUEUE(R.bool.feature_reload_last_playqueue);
    private final int resourceId;

    private Flag(int resourceId) {
        this.resourceId = resourceId;
    }

    public int getId() {
        return resourceId;
    }

}
