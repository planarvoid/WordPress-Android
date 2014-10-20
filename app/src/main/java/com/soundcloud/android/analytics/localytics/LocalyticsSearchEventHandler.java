package com.soundcloud.android.analytics.localytics;

import com.localytics.android.LocalyticsSession;
import com.soundcloud.android.events.SearchEvent;

class LocalyticsSearchEventHandler {

    private final LocalyticsSession localyticsSession;

    public LocalyticsSearchEventHandler(LocalyticsSession localyticsSession) {
        this.localyticsSession = localyticsSession;
    }

    public void handleEvent(SearchEvent event) {
        switch (event.getKind()) {
            case SearchEvent.KIND_SUGGESTION:
                localyticsSession.tagEvent(LocalyticsEvents.Search.SEARCH_SUGGESTION, event.getAttributes());
                break;
            case SearchEvent.KIND_SUBMIT:
                localyticsSession.tagEvent(LocalyticsEvents.Search.SEARCH_SUBMIT, event.getAttributes());
                break;
            case SearchEvent.KIND_RESULTS:
                localyticsSession.tagEvent(LocalyticsEvents.Search.SEARCH_RESULTS, event.getAttributes());
                break;
        }
    }
}
