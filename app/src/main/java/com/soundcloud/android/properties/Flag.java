package com.soundcloud.android.properties;

import com.soundcloud.android.R;

import java.util.EnumSet;

public enum Flag {

    TEST_FEATURE(-1),
    PAYMENTS_TEST(R.bool.feature_payments_test),
    GOOGLE_CAST(R.bool.feature_google_cast),
    OFFLINE_SYNC(R.bool.feature_offline_sync),
    PROMOTED_IN_STREAM(R.bool.feature_promoted_in_stream),
    NEW_PROFILE(R.bool.feature_new_profile),
    FOLLOW_USER_SEARCH(R.bool.feature_follow_user_search),
    NEW_PROFILE_FRAGMENTS(R.bool.feature_new_profile_fragments),
    PLAY_RELATED_TRACKS(R.bool.feature_play_related_tracks),
    NEVER_ENDING_PLAY_QUEUE(R.bool.feature_never_ending_playqueue),
    STATIONS(R.bool.feature_stations);

    private final int resourceId;

    Flag(int resourceId) {
        this.resourceId = resourceId;
    }

    public int getId() {
        return resourceId;
    }

    public static EnumSet<Flag> realFeatures(){
        return EnumSet.complementOf(EnumSet.of(TEST_FEATURE, PAYMENTS_TEST));
    }

}
