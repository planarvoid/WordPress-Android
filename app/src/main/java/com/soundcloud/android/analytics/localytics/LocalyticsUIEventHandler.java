package com.soundcloud.android.analytics.localytics;

import com.localytics.android.LocalyticsSession;
import com.soundcloud.android.events.UIEvent;

import java.util.Map;

class LocalyticsUIEventHandler {

    private LocalyticsSession localyticsSession;

    LocalyticsUIEventHandler(LocalyticsSession localyticsSession) {
        this.localyticsSession = localyticsSession;
    }

    public void handleEvent(UIEvent sourceEvent) {
        handleEvent(sourceEvent.getKind(), sourceEvent.getAttributes());
    }

    private void handleEvent(int sourceEventType, Map<String, String> eventAttributes) {
        switch (sourceEventType) {
            case UIEvent.FOLLOW:
                localyticsSession.tagEvent(LocalyticsEvents.UI.FOLLOW, eventAttributes);
                break;
            case UIEvent.UNFOLLOW:
                localyticsSession.tagEvent(LocalyticsEvents.UI.UNFOLLOW, eventAttributes);
                break;
            case UIEvent.LIKE:
                localyticsSession.tagEvent(LocalyticsEvents.UI.LIKE, eventAttributes);
                break;
            case UIEvent.UNLIKE:
                localyticsSession.tagEvent(LocalyticsEvents.UI.UNLIKE, eventAttributes);
                break;
            case UIEvent.REPOST:
                localyticsSession.tagEvent(LocalyticsEvents.UI.REPOST, eventAttributes);
                break;
            case UIEvent.UNREPOST:
                localyticsSession.tagEvent(LocalyticsEvents.UI.UNREPOST, eventAttributes);
                break;
            case UIEvent.ADD_TO_PLAYLIST:
                localyticsSession.tagEvent(LocalyticsEvents.UI.ADD_TO_PLAYLIST, eventAttributes);
                break;
            case UIEvent.COMMENT:
                localyticsSession.tagEvent(LocalyticsEvents.UI.COMMENT, eventAttributes);
                break;
            case UIEvent.SHARE:
                localyticsSession.tagEvent(LocalyticsEvents.UI.SHARE, eventAttributes);
                break;
            case UIEvent.SHUFFLE_LIKES:
                localyticsSession.tagEvent(LocalyticsEvents.UI.SHUFFLE_LIKES, eventAttributes);
                break;
            case UIEvent.NAVIGATION:
                localyticsSession.tagEvent(LocalyticsEvents.UI.NAVIGATION, eventAttributes);
                break;
            default:
                throw new IllegalArgumentException("UI Event type is invalid");
        }
    }

}
