package com.soundcloud.android.analytics.localytics;

import com.localytics.android.LocalyticsSession;
import com.soundcloud.android.events.SearchEvent;

class LocalyticsSearchEventHandler {

    private final LocalyticsSession mLocalyticsSession;

    public LocalyticsSearchEventHandler(LocalyticsSession localyticsSession) {
        mLocalyticsSession = localyticsSession;
    }

    public void handleEvent(SearchEvent event) {
        switch (event.getKind()) {
            case SearchEvent.SEARCH_SUGGESTION:
                mLocalyticsSession.tagEvent(LocalyticsEvents.Search.SEARCH_SUGGESTION, event.getAttributes());
                break;
            case SearchEvent.SEARCH_SUBMIT:
                mLocalyticsSession.tagEvent(LocalyticsEvents.Search.SEARCH_SUBMIT, event.getAttributes());
                break;
            case SearchEvent.SEARCH_RESULTS:
                mLocalyticsSession.tagEvent(LocalyticsEvents.Search.SEARCH_RESULTS, event.getAttributes());
                break;
        }
    }
}
