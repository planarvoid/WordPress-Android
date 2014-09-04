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

    private void handleEvent(UIEvent.Kind sourceEventType, Map<String, String> eventAttributes) {
        switch (sourceEventType) {
            case FOLLOW:
                localyticsSession.tagEvent(LocalyticsEvents.UI.FOLLOW, eventAttributes);
                break;
            case UNFOLLOW:
                localyticsSession.tagEvent(LocalyticsEvents.UI.UNFOLLOW, eventAttributes);
                break;
            case LIKE:
                localyticsSession.tagEvent(LocalyticsEvents.UI.LIKE, eventAttributes);
                break;
            case UNLIKE:
                localyticsSession.tagEvent(LocalyticsEvents.UI.UNLIKE, eventAttributes);
                break;
            case REPOST:
                localyticsSession.tagEvent(LocalyticsEvents.UI.REPOST, eventAttributes);
                break;
            case UNREPOST:
                localyticsSession.tagEvent(LocalyticsEvents.UI.UNREPOST, eventAttributes);
                break;
            case ADD_TO_PLAYLIST:
                localyticsSession.tagEvent(LocalyticsEvents.UI.ADD_TO_PLAYLIST, eventAttributes);
                break;
            case COMMENT:
                localyticsSession.tagEvent(LocalyticsEvents.UI.COMMENT, eventAttributes);
                break;
            case SHARE:
                localyticsSession.tagEvent(LocalyticsEvents.UI.SHARE, eventAttributes);
                break;
            case SHUFFLE_LIKES:
                localyticsSession.tagEvent(LocalyticsEvents.UI.SHUFFLE_LIKES, eventAttributes);
                break;
            case NAVIGATION:
                localyticsSession.tagEvent(LocalyticsEvents.UI.NAVIGATION, eventAttributes);
                break;
            case PLAYER_OPEN:
                localyticsSession.tagEvent(LocalyticsEvents.UI.PLAYER_OPEN, eventAttributes);
                break;
            case PLAYER_CLOSE:
                localyticsSession.tagEvent(LocalyticsEvents.UI.PLAYER_CLOSE, eventAttributes);
                break;
            default:
                break;
        }
    }

}
