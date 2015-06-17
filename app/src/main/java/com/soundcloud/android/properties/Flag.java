package com.soundcloud.android.properties;

import com.soundcloud.android.R;

public enum Flag {

    TEST_FEATURE(-1),
    PAYMENTS_TEST(R.bool.feature_payments_test),
    GOOGLE_CAST(R.bool.feature_google_cast),
    OFFLINE_SYNC(R.bool.feature_offline_sync),
    PROMOTED_IN_STREAM(R.bool.feature_promoted_in_stream),
    NEW_PROFILE(R.bool.feature_new_profile),
    FOLLOW_USER_SEARCH(R.bool.feature_follow_user_search),
    NEW_PROFILE_FRAGMENTS(R.bool.feature_new_profile_fragments),
    EVERYBODY_GETS_SKIPPY(R.bool.feature_everyone_gets_skippy);

    private final int resourceId;

    Flag(int resourceId) {
        this.resourceId = resourceId;
    }

    public int getId() {
        return resourceId;
    }

}
