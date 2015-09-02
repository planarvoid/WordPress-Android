package com.soundcloud.android.analytics.localytics;

import android.support.v4.util.ArrayMap;

import com.localytics.android.LocalyticsSession;
import com.soundcloud.android.events.LocalyticTrackingKeys;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.utils.Log;
import com.soundcloud.java.objects.MoreObjects;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

class LocalyticsUIEventHandler {

    public static final String TAG = "LocalyticsUIHandler";

    private static final List<String> likeToggleKeys = Arrays.asList(
            LocalyticTrackingKeys.KEY_CONTEXT,
            LocalyticTrackingKeys.KEY_LOCATION,
            LocalyticTrackingKeys.KEY_RESOURCES_TYPE,
            LocalyticTrackingKeys.KEY_RESOURCE_ID);

    private static final List<String> repostToggleKeys = Arrays.asList(
            LocalyticTrackingKeys.KEY_CONTEXT,
            LocalyticTrackingKeys.KEY_RESOURCES_TYPE,
            LocalyticTrackingKeys.KEY_RESOURCE_ID);

    private final LocalyticsSession localyticsSession;

    LocalyticsUIEventHandler(LocalyticsSession localyticsSession) {
        this.localyticsSession = localyticsSession;
    }

    public void handleEvent(UIEvent event) {
        switch (event.getKind()) {
            case UIEvent.KIND_FOLLOW:
                tagEvent(LocalyticsEvents.UI.FOLLOW, event.getAttributes());
                break;
            case UIEvent.KIND_LIKE:
                tagEvent(LocalyticsEvents.UI.LIKE, filterKeysFromAttributes(likeToggleKeys, event.getAttributes()));
                break;
            case UIEvent.KIND_UNLIKE:
                tagEvent(LocalyticsEvents.UI.UNLIKE, filterKeysFromAttributes(likeToggleKeys, event.getAttributes()));
                break;
            case UIEvent.KIND_REPOST:
                tagEvent(LocalyticsEvents.UI.REPOST, filterKeysFromAttributes(repostToggleKeys, event.getAttributes()));
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
            default:
                break;
        }
    }

    private Map<String, String> filterKeysFromAttributes(List<String> filterKeys, Map<String, String> attributes) {
        final Map<String, String> filteredAttributes = new ArrayMap<>();
        for (String key : filterKeys) {
            if (attributes.containsKey(key)) {
                filteredAttributes.put(key, attributes.get(key));
            }
        }
        return filteredAttributes;
    }

    private void tagEvent(String tagName, Map<String, String> attributes) {
        logAttributes(tagName, attributes);
        localyticsSession.tagEvent(tagName, attributes);
    }

    private void logAttributes(String tagName, Map<String, String> eventAttributes) {
        if (android.util.Log.isLoggable(TAG, android.util.Log.DEBUG)) {
            final MoreObjects.ToStringHelper toStringHelper = MoreObjects.toStringHelper(tagName + " with EventAttributes");
            for (String key : eventAttributes.keySet()) {
                toStringHelper.add(key, eventAttributes.get(key));
            }
            Log.d(TAG, toStringHelper.toString());
        }
    }

}
