package com.soundcloud.android.analytics.localytics;

import com.localytics.android.LocalyticsSession;
import com.soundcloud.android.events.SocialEvent;

import java.util.Map;

class LocalyticsSocialEventHandler {
    private LocalyticsSession mLocalyticsSession;

    LocalyticsSocialEventHandler(LocalyticsSession localyticsSession) {
        mLocalyticsSession = localyticsSession;
    }

    public void handleEvent(SocialEvent sourceEvent) {
        handleEvent(sourceEvent.getType(), sourceEvent.getAttributes());
    }

    private void handleEvent(int sourceEventType, Map<String, String> eventAttributes) {
        switch (sourceEventType) {
            case SocialEvent.TYPE_FOLLOW:
                mLocalyticsSession.tagEvent(LocalyticsEvents.Social.FOLLOW, eventAttributes);
                break;
            case SocialEvent.TYPE_UNFOLLOW:
                mLocalyticsSession.tagEvent(LocalyticsEvents.Social.UNFOLLOW, eventAttributes);
                break;
            case SocialEvent.TYPE_LIKE:
                mLocalyticsSession.tagEvent(LocalyticsEvents.Social.LIKE, eventAttributes);
                break;
            case SocialEvent.TYPE_UNLIKE:
                mLocalyticsSession.tagEvent(LocalyticsEvents.Social.UNLIKE, eventAttributes);
                break;
            case SocialEvent.TYPE_REPOST:
                mLocalyticsSession.tagEvent(LocalyticsEvents.Social.REPOST, eventAttributes);
                break;
            case SocialEvent.TYPE_UNREPOST:
                mLocalyticsSession.tagEvent(LocalyticsEvents.Social.UNREPOST, eventAttributes);
                break;
            case SocialEvent.TYPE_ADD_TO_PLAYLIST:
                mLocalyticsSession.tagEvent(LocalyticsEvents.Social.ADD_TO_PLAYLIST, eventAttributes);
                break;
            case SocialEvent.TYPE_COMMENT:
                mLocalyticsSession.tagEvent(LocalyticsEvents.Social.COMMENT, eventAttributes);
                break;
            case SocialEvent.TYPE_SHARE:
                mLocalyticsSession.tagEvent(LocalyticsEvents.Social.SHARE, eventAttributes);
                break;
            default:
                throw new IllegalArgumentException("Social Event type is invalid");
        }
    }

}
