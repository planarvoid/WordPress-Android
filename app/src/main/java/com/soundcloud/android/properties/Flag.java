package com.soundcloud.android.properties;

import com.soundcloud.android.R;

public enum Flag {

    TEST_FEATURE(-1),
    PAYMENTS_TEST(R.bool.feature_payments_test),
    GOOGLE_CAST(R.bool.feature_google_cast),
    OFFLINE_SYNC(R.bool.feature_offline_sync),
    PROMOTED_IN_STREAM(R.bool.feature_promoted_in_stream),
    NEW_PROFILE_FRAGMENTS(R.bool.new_profile_fragments),
    EVENTLOGGER_SEARCH_EVENTS(R.bool.feature_eventlogger_search);

    private final int resourceId;

    private Flag(int resourceId) {
        this.resourceId = resourceId;
    }

    public int getId() {
        return resourceId;
    }

}
