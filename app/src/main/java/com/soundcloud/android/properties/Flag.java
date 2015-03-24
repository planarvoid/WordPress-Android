package com.soundcloud.android.properties;

import com.soundcloud.android.R;

public enum Flag {

    TEST_FEATURE(-1),
    PAYMENTS(R.bool.feature_payments),
    PAYMENTS_TEST(R.bool.feature_payments_test),
    GOOGLE_CAST(R.bool.feature_google_cast),
    CONFIGURATION_FEATURES(R.bool.feature_configuration_features),
    RELOAD_LAST_PLAYQUEUE(R.bool.feature_reload_last_playqueue),
    EVENTLOGGER_PAGE_VIEW_EVENTS(R.bool.feature_eventlogger_page_view),
    NEW_PLAYLIST_ENGAGEMENTS(R.bool.feature_new_playlist_engagements),
    EVENTLOGGER_BATCHING(R.bool.feature_eventlogger_batching),
    EVENTLOGGER_SEARCH_EVENTS(R.bool.feature_eventlogger_search);

    private final int resourceId;

    private Flag(int resourceId) {
        this.resourceId = resourceId;
    }

    public int getId() {
        return resourceId;
    }

}
