package com.soundcloud.android.analytics.localytics;

import com.google.common.annotations.VisibleForTesting;
import com.localytics.android.LocalyticsSession;
import com.soundcloud.android.events.SocialEvent;

import java.util.HashMap;
import java.util.Map;

class LocalyticsSocialEventHandler {
    private LocalyticsSession mLocalyticsSession;

    LocalyticsSocialEventHandler(LocalyticsSession localyticsSession) {
        mLocalyticsSession = localyticsSession;
    }

    public void handleEvent(SocialEvent sourceEvent) {
        handleEvent(sourceEvent.getType(), sourceEvent.getAttributes());
    }

    @VisibleForTesting
    void handleEvent(int sourceEventType, SocialEvent.Attributes sourceEventAttributes) {
        Map<String, String> mappedEventAttributes = new HashMap<String, String>();
        switch (sourceEventType) {
            case SocialEvent.TYPE_FOLLOW:
                handleEventFollow(sourceEventAttributes, mappedEventAttributes);
                break;
            case SocialEvent.TYPE_LIKE:
                handleEventLike(sourceEventAttributes, mappedEventAttributes);
                break;
            case SocialEvent.TYPE_REPOST:
                handleEventRepost(sourceEventAttributes, mappedEventAttributes);
                break;
            case SocialEvent.TYPE_ADD_TO_PLAYLIST:
                handleEventAddToPlaylist(sourceEventAttributes, mappedEventAttributes);
                break;
            case SocialEvent.TYPE_COMMENT:
                handleEventComment(sourceEventAttributes, mappedEventAttributes);
                break;
            case SocialEvent.TYPE_SHARE:
                handleEventShare(sourceEventAttributes, mappedEventAttributes);
                break;
            default:
                throw new IllegalArgumentException("Social Event type is invalid");
        }
    }

    @VisibleForTesting
    void handleEventFollow(SocialEvent.Attributes sourceAttributes, Map<String, String> eventAttributes) {
        eventAttributes.put("context", sourceAttributes.screenTag);
        eventAttributes.put("user_id", String.valueOf(sourceAttributes.userId));
        mLocalyticsSession.tagEvent(LocalyticsEvents.Social.FOLLOW, eventAttributes);
    }

    @VisibleForTesting
    void handleEventLike(SocialEvent.Attributes sourceAttributes, Map<String, String> eventAttributes) {
        eventAttributes.put("context", sourceAttributes.screenTag);
        eventAttributes.put("resource", sourceAttributes.resource);
        eventAttributes.put("resource_id", String.valueOf(sourceAttributes.resourceId));
        mLocalyticsSession.tagEvent(LocalyticsEvents.Social.LIKE, eventAttributes);
    }

    @VisibleForTesting
    void handleEventRepost(SocialEvent.Attributes sourceAttributes, Map<String, String> eventAttributes) {
        eventAttributes.put("context", sourceAttributes.screenTag);
        eventAttributes.put("resource", sourceAttributes.resource);
        eventAttributes.put("resource_id", String.valueOf(sourceAttributes.resourceId));
        mLocalyticsSession.tagEvent(LocalyticsEvents.Social.REPOST, eventAttributes);
    }

    @VisibleForTesting
    void handleEventAddToPlaylist(SocialEvent.Attributes sourceAttributes, Map<String, String> eventAttributes) {
        eventAttributes.put("context", sourceAttributes.screenTag);
        eventAttributes.put("new_playlist", sourceAttributes.isNewPlaylist ? "yes" : "no" );
        eventAttributes.put("track_id", String.valueOf(sourceAttributes.trackId));
        mLocalyticsSession.tagEvent(LocalyticsEvents.Social.ADD_TO_PLAYLIST, eventAttributes);
    }

    @VisibleForTesting
    void handleEventComment(SocialEvent.Attributes sourceAttributes, Map<String, String> eventAttributes) {
        eventAttributes.put("context", sourceAttributes.screenTag);
        eventAttributes.put("track_id", String.valueOf(sourceAttributes.trackId));
        mLocalyticsSession.tagEvent(LocalyticsEvents.Social.COMMENT, eventAttributes);
    }

    @VisibleForTesting
    void handleEventShare(SocialEvent.Attributes sourceAttributes, Map<String, String> eventAttributes) {
        eventAttributes.put("context", sourceAttributes.screenTag);
        eventAttributes.put("resource", sourceAttributes.resource);
        eventAttributes.put("resource_id", String.valueOf(sourceAttributes.resourceId));
        eventAttributes.put("shared_to", sourceAttributes.sharedTo);
        mLocalyticsSession.tagEvent(LocalyticsEvents.Social.SHARE, eventAttributes);
    }
}
