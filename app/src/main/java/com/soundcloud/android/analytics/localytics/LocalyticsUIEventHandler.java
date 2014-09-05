package com.soundcloud.android.analytics.localytics;

import com.google.common.base.Objects;
import com.localytics.android.LocalyticsSession;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.utils.Log;

import java.util.Map;

class LocalyticsUIEventHandler {

    public static final String TAG = "LocalyticsUIHandler";

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
                tagEvent(LocalyticsEvents.UI.FOLLOW, eventAttributes);
                break;
            case UNFOLLOW:
                tagEvent(LocalyticsEvents.UI.UNFOLLOW, eventAttributes);
                break;
            case LIKE:
                tagEvent(LocalyticsEvents.UI.LIKE, eventAttributes);
                break;
            case UNLIKE:
                tagEvent(LocalyticsEvents.UI.UNLIKE, eventAttributes);
                break;
            case REPOST:
                tagEvent(LocalyticsEvents.UI.REPOST, eventAttributes);
                break;
            case UNREPOST:
                tagEvent(LocalyticsEvents.UI.UNREPOST, eventAttributes);
                break;
            case ADD_TO_PLAYLIST:
                tagEvent(LocalyticsEvents.UI.ADD_TO_PLAYLIST, eventAttributes);
                break;
            case COMMENT:
                tagEvent(LocalyticsEvents.UI.COMMENT, eventAttributes);
                break;
            case SHARE:
                tagEvent(LocalyticsEvents.UI.SHARE, eventAttributes);
                break;
            case SHUFFLE_LIKES:
                tagEvent(LocalyticsEvents.UI.SHUFFLE_LIKES, eventAttributes);
                break;
            case NAVIGATION:
                tagEvent(LocalyticsEvents.UI.NAVIGATION, eventAttributes);
                break;
            case PLAYER_OPEN:
                tagEvent(LocalyticsEvents.UI.PLAYER_OPEN, eventAttributes);
                break;
            case PLAYER_CLOSE:
                tagEvent(LocalyticsEvents.UI.PLAYER_CLOSE, eventAttributes);
                break;
            default:
                break;
        }
    }

    private void tagEvent(String tagName, Map<String, String> attributes) {
        logAttributes(tagName, attributes);
        localyticsSession.tagEvent(tagName, attributes);
    }

    private void logAttributes(String tagName, Map<String, String> eventAttributes) {
        if (android.util.Log.isLoggable(TAG, android.util.Log.DEBUG)) {
            final Objects.ToStringHelper toStringHelper = Objects.toStringHelper(tagName + " with EventAttributes");
            for (String key : eventAttributes.keySet()) {
                toStringHelper.add(key, eventAttributes.get(key));
            }
            Log.d(TAG, toStringHelper.toString());
        }
    }

}
