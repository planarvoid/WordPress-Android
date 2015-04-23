package com.soundcloud.android.properties;

import com.soundcloud.android.R;

public enum Flag {

    TEST_FEATURE(-1),
    PAYMENTS_TEST(R.bool.feature_payments_test),
    GOOGLE_CAST(R.bool.feature_google_cast),
    OFFLINE_SYNC(R.bool.feature_offline_sync),
    RELOAD_LAST_PLAYQUEUE(R.bool.feature_reload_last_playqueue),
    PROMOTED_IN_STREAM(R.bool.feature_promoted_in_stream),
    EVENTLOGGER_SEARCH_EVENTS(R.bool.feature_eventlogger_search);

    private final int resourceId;

    private Flag(int resourceId) {
        this.resourceId = resourceId;
    }

    public int getId() {
        return resourceId;
    }

}
