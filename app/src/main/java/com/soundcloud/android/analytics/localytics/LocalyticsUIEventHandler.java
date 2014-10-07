package com.soundcloud.android.analytics.localytics;

import com.google.common.base.Objects;
import com.localytics.android.LocalyticsSession;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.utils.Log;

import java.util.Map;

class LocalyticsUIEventHandler {

    public static final String TAG = "LocalyticsUIHandler";

    private final LocalyticsSession localyticsSession;

    LocalyticsUIEventHandler(LocalyticsSession localyticsSession) {
        this.localyticsSession = localyticsSession;
    }

    public void handleEvent(UIEvent event) {
        switch (event.getKind()) {
            case UIEvent.KIND_FOLLOW:
                tagEvent(LocalyticsEvents.UI.FOLLOW, event.getAttributes());
                break;
            case UIEvent.KIND_UNFOLLOW:
                tagEvent(LocalyticsEvents.UI.UNFOLLOW, event.getAttributes());
                break;
            case UIEvent.KIND_LIKE:
                tagEvent(LocalyticsEvents.UI.LIKE, event.getAttributes());
                break;
            case UIEvent.KIND_UNLIKE:
                tagEvent(LocalyticsEvents.UI.UNLIKE, event.getAttributes());
                break;
            case UIEvent.KIND_REPOST:
                tagEvent(LocalyticsEvents.UI.REPOST, event.getAttributes());
                break;
            case UIEvent.KIND_UNREPOST:
                tagEvent(LocalyticsEvents.UI.UNREPOST, event.getAttributes());
                break;
            case UIEvent.KIND_ADD_TO_PLAYLIST:
                tagEvent(LocalyticsEvents.UI.ADD_TO_PLAYLIST, event.getAttributes());
                break;
            case UIEvent.KIND_COMMENT:
                tagEvent(LocalyticsEvents.UI.COMMENT, event.getAttributes());
                break;
            case UIEvent.KIND_SHARE:
                tagEvent(LocalyticsEvents.UI.SHARE, event.getAttributes());
                break;
            case UIEvent.KIND_SHUFFLE_LIKES:
                tagEvent(LocalyticsEvents.UI.SHUFFLE_LIKES, event.getAttributes());
                break;
            case UIEvent.KIND_NAVIGATION:
                tagEvent(LocalyticsEvents.UI.NAVIGATION, event.getAttributes());
                break;
            case UIEvent.KIND_PLAYER_OPEN:
                tagEvent(LocalyticsEvents.UI.PLAYER_OPEN, event.getAttributes());
                break;
            case UIEvent.KIND_PLAYER_CLOSE:
                tagEvent(LocalyticsEvents.UI.PLAYER_CLOSE, event.getAttributes());
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
